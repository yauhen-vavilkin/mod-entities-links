package org.folio.entlinks.integration;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.model.projection.LinkCountView;
import org.folio.entlinks.repository.InstanceLinkRepository;
import org.folio.entlinks.service.AuthorityChangeHandlingService;
import org.folio.qm.domain.dto.AuthorityInventoryRecord;
import org.folio.qm.domain.dto.InventoryEvent;
import org.folio.spring.test.type.UnitTest;
import org.folio.spring.tools.batch.MessageBatchProcessor;
import org.folio.spring.tools.systemuser.SystemUserScopedExecutionService;
import org.folio.support.TestUtils;
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
  private InstanceLinkRepository repository;
  @Mock
  private SystemUserScopedExecutionService executionService;
  @Mock
  private AuthorityChangeHandlingService authorityChangeHandlingService;
  @Mock
  private MessageBatchProcessor messageBatchProcessor;

  @Mock
  private ConsumerRecord<String, InventoryEvent> consumerRecord;

  @InjectMocks
  private AuthorityEventListener listener;

  @BeforeEach
  void setUp() {
    when(executionService.executeSystemUserScoped(any(), any())).thenAnswer(invocation -> {
      var argument = invocation.getArgument(1, Callable.class);
      return argument.call();
    });
  }

  @SuppressWarnings("unchecked")
  private void mockSuccessHandling() {
    doAnswer(invocation -> {
      var argument = invocation.getArgument(2, Consumer.class);
      var batch = invocation.getArgument(0, List.class);
      argument.accept(batch);
      return null;
    }).when(messageBatchProcessor).consumeBatchWithFallback(any(), any(), any(), any());
  }

  @SuppressWarnings("unchecked")
  private void mockFailedHandling(Exception e) {
    doAnswer(invocation -> {
      var argument = invocation.getArgument(3, BiConsumer.class);
      var batch = invocation.getArgument(0, List.class);
      argument.accept(batch.get(0), e);
      return null;
    }).when(messageBatchProcessor).consumeBatchWithFallback(any(), any(), any(), any());
  }

  @ValueSource(strings = {"UPDATE", "DELETE"})
  @ParameterizedTest
  void shouldHandleEvent_positive_whenLinksExists(String type) {
    var authId = UUID.randomUUID();
    var newRecord = new AuthorityInventoryRecord().id(authId);
    var oldRecord = new AuthorityInventoryRecord().id(authId);
    var event = TestUtils.authorityEvent(type, newRecord, oldRecord);

    mockSuccessHandling();
    when(consumerRecord.key()).thenReturn(authId.toString());
    when(consumerRecord.value()).thenReturn(event);
    when(repository.countLinksByAuthorityIds(List.of(authId))).thenReturn(singletonList(new LinksCount(authId, 1L)));

    listener.handleEvents(singletonList(consumerRecord));

    verify(authorityChangeHandlingService).handleAuthoritiesChanges(singletonList(event));
  }

  @ValueSource(strings = {"UPDATE", "DELETE"})
  @ParameterizedTest
  void shouldNotHandleEvent_positive_whenNoLinksExists(String type) {
    var authId = UUID.randomUUID();
    var newRecord = new AuthorityInventoryRecord().id(authId);
    var oldRecord = new AuthorityInventoryRecord().id(authId);
    var event = TestUtils.authorityEvent(type, newRecord, oldRecord);

    mockSuccessHandling();
    when(consumerRecord.key()).thenReturn(authId.toString());
    when(consumerRecord.value()).thenReturn(event);
    when(repository.countLinksByAuthorityIds(List.of(authId))).thenReturn(emptyList());

    listener.handleEvents(singletonList(consumerRecord));

    verify(authorityChangeHandlingService, never()).handleAuthoritiesChanges(singletonList(event));
  }

  @Test
  void shouldNotHandleEvent_negative_whenExceptionOccurred() {
    var authId = UUID.randomUUID();
    var newRecord = new AuthorityInventoryRecord().id(authId);
    var oldRecord = new AuthorityInventoryRecord().id(authId);
    var event = TestUtils.authorityEvent("UPDATE", newRecord, oldRecord);

    mockFailedHandling(new RuntimeException("test message"));
    when(consumerRecord.key()).thenReturn(authId.toString());
    when(consumerRecord.value()).thenReturn(event);
    when(repository.countLinksByAuthorityIds(List.of(authId))).thenReturn(singletonList(new LinksCount(authId, 1L)));

    listener.handleEvents(singletonList(consumerRecord));

    verify(authorityChangeHandlingService, never()).handleAuthoritiesChanges(singletonList(event));
  }


  record LinksCount(UUID id, Long totalLinks) implements LinkCountView {

    @Override
    public UUID getId() {
      return id();
    }

    @Override
    public Long getTotalLinks() {
      return totalLinks();
    }
  }
}
