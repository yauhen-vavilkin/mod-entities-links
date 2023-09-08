package org.folio.entlinks.integration.kafka;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.folio.support.MockingTestUtils.mockBatchFailedHandling;
import static org.folio.support.MockingTestUtils.mockBatchSuccessHandling;
import static org.folio.support.TestDataUtils.report;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.test.type.UnitTest;
import org.folio.spring.tools.batch.MessageBatchProcessor;
import org.folio.support.KafkaTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityStatsEventListenerTest {

  @Mock
  private SystemUserScopedExecutionService executionService;
  @Mock
  private AuthorityDataStatService dataStatService;
  @Mock
  private MessageBatchProcessor messageBatchProcessor;

  @InjectMocks
  private InstanceAuthorityStatsEventListener listener;

  @BeforeEach
  void setUp() {
    when(executionService.executeSystemUserScoped(any(), any())).thenAnswer(invocation -> {
      var argument = invocation.getArgument(1, Callable.class);
      return argument.call();
    });
  }

  // Test that multiple tenants processed in different batches and jobIds in different sub-batches
  @Test
  void shouldHandleEvent_positive() {
    var tenant1 = randomAlphabetic(10);
    var tenant2 = randomAlphabetic(10);
    var job1Id = UUID.randomUUID();
    var job2Id = UUID.randomUUID();
    var reports = List.of(
      report(tenant1, job1Id),
      report(tenant1, job2Id),
      report(tenant2, job1Id),
      report(tenant2, job1Id)
    );
    var consumerRecords = KafkaTestUtils.consumerRecords(reports);

    mockBatchSuccessHandling(messageBatchProcessor);

    listener.handleEvents(consumerRecords);

    verify(messageBatchProcessor, times(2))
      .consumeBatchWithFallback(any(), any(), any(), any());

    verify(dataStatService)
      .updateForReports(job1Id, singletonList(reports.get(0)));
    verify(dataStatService)
      .updateForReports(job2Id, singletonList(reports.get(1)));
    verify(dataStatService)
      .updateForReports(job1Id, List.of(reports.get(2), reports.get(3)));
  }

  @Test
  void shouldNotHandleEvent_negative_whenExceptionOccurred() {
    var report = report(randomAlphabetic(10), UUID.randomUUID());
    var consumerRecords = KafkaTestUtils.consumerRecords(singletonList(report));

    mockBatchFailedHandling(messageBatchProcessor, new RuntimeException("test message"));

    listener.handleEvents(consumerRecords);

    verifyNoInteractions(dataStatService);
  }

}
