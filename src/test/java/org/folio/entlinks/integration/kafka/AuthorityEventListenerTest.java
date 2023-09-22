package org.folio.entlinks.integration.kafka;

import static java.util.Collections.singletonList;
import static org.folio.support.MockingTestUtils.mockBatchFailedHandling;
import static org.folio.support.MockingTestUtils.mockBatchSuccessHandling;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.Callable;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.Metadata;
import org.folio.entlinks.integration.dto.AuthorityDomainEvent;
import org.folio.entlinks.service.messaging.authority.InstanceAuthorityLinkUpdateService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.test.type.UnitTest;
import org.folio.spring.tools.batch.MessageBatchProcessor;
import org.folio.support.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityEventListenerTest {

  @Mock
  private SystemUserScopedExecutionService executionService;
  @Mock
  private InstanceAuthorityLinkUpdateService instanceAuthorityLinkUpdateService;
  @Mock
  private MessageBatchProcessor messageBatchProcessor;

  @Mock
  private ConsumerRecord<String, AuthorityDomainEvent> consumerRecord;

  @InjectMocks
  private AuthorityEventListener listener;

  @BeforeEach
  void setUp() {
    when(executionService.executeSystemUserScoped(any(), any())).thenAnswer(invocation -> {
      var argument = invocation.getArgument(1, Callable.class);
      return argument.call();
    });
  }

  @ValueSource(strings = {"UPDATE", "DELETE"})
  @ParameterizedTest
  void shouldHandleEvent_positive_whenLinksExists(String type) {
    var authId = UUID.randomUUID();
    var newRecord = new AuthorityDto().id(authId);
    var oldRecord = new AuthorityDto().id(authId);
    var event = TestDataUtils.authorityEvent(type, newRecord, oldRecord);

    mockBatchSuccessHandling(messageBatchProcessor);
    when(consumerRecord.key()).thenReturn(authId.toString());
    when(consumerRecord.value()).thenReturn(event);

    listener.handleEvents(singletonList(consumerRecord));

    verify(instanceAuthorityLinkUpdateService).handleAuthoritiesChanges(singletonList(event));
  }

  @ValueSource(strings = {"UPDATE", "DELETE"})
  @ParameterizedTest
  void shouldHandleEvent_positive_whenNoLinksExists(String type) {
    var authId = UUID.randomUUID();
    var updatedByUserId = UUID.randomUUID();
    var meta = new Metadata().updatedByUserId(updatedByUserId);
    var newRecord = new AuthorityDto().id(authId).metadata(meta);
    var oldRecord = new AuthorityDto().id(authId).metadata(meta.updatedByUserId(updatedByUserId));
    var event = TestDataUtils.authorityEvent(type, newRecord, oldRecord);

    mockBatchSuccessHandling(messageBatchProcessor);
    when(consumerRecord.key()).thenReturn(authId.toString());
    when(consumerRecord.value()).thenReturn(event);

    listener.handleEvents(singletonList(consumerRecord));

    verify(instanceAuthorityLinkUpdateService).handleAuthoritiesChanges(singletonList(event));
  }

  @Test
  void shouldNotHandleEvent_negative_whenExceptionOccurred() {
    var authId = UUID.randomUUID();
    var newRecord = new AuthorityDto().id(authId);
    var oldRecord = new AuthorityDto().id(authId);
    var event = TestDataUtils.authorityEvent("UPDATE", newRecord, oldRecord);

    mockBatchFailedHandling(messageBatchProcessor, new RuntimeException("test message"));
    when(consumerRecord.key()).thenReturn(authId.toString());
    when(consumerRecord.value()).thenReturn(event);

    listener.handleEvents(singletonList(consumerRecord));

    verify(instanceAuthorityLinkUpdateService, never()).handleAuthoritiesChanges(singletonList(event));
  }

}
