package org.folio.entlinks.service.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.reindex.ReindexContext;
import org.folio.entlinks.service.reindex.event.DomainEvent;
import org.folio.entlinks.service.reindex.event.DomainEventType;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityDomainEventPublisherTest {

  private static final String TENANT_ID = "test";
  private static final String DOMAIN_EVENT_TYPE_HEADER = "domain-event-type";

  @Mock
  private EventProducer<DomainEvent<?>> eventProducer;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @InjectMocks
  private AuthorityDomainEventPublisher eventPublisher;

  private final ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);

  @Test
  void shouldNotSendCreatedEventWhenIdIsNull() {
    // when
    eventPublisher.publishCreateEvent(new AuthorityDto());

    // then
    verifyNoInteractions(eventProducer);
  }

  @Test
  void shouldSendCreatedEvent() {
    // given
    var dto = new AuthorityDto().id(UUID.randomUUID()).source("source");
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);

    // when
    eventPublisher.publishCreateEvent(dto);

    // then
    verify(eventProducer).sendMessage(eq(dto.getId().toString()), captor.capture(), eq(DOMAIN_EVENT_TYPE_HEADER),
        eq(DomainEventType.CREATE));
    assertEquals(dto, captor.getValue().getNewEntity());
    assertEquals(TENANT_ID, captor.getValue().getTenant());
  }

  @Test
  void shouldNotSendUpdatedEventWhenIdIsNull() {
    // when
    eventPublisher.publishUpdateEvent(new AuthorityDto(), new AuthorityDto());

    // then
    verifyNoInteractions(eventProducer);
  }

  @Test
  void shouldSendUpdatedEvent() {
    // given
    var id = UUID.randomUUID();
    var oldDto = new AuthorityDto().id(id).source("sourceOld");
    var newDto = new AuthorityDto().id(id).source("sourceNew");
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);

    // when
    eventPublisher.publishUpdateEvent(oldDto, newDto);

    // then
    verify(eventProducer).sendMessage(eq(oldDto.getId().toString()), captor.capture(), eq(DOMAIN_EVENT_TYPE_HEADER),
        eq(DomainEventType.UPDATE));
    assertEquals(newDto, captor.getValue().getNewEntity());
    assertEquals(oldDto, captor.getValue().getOldEntity());
    assertEquals(TENANT_ID, captor.getValue().getTenant());
  }

  @Test
  void shouldNotSendDeletedEventWhenIdIsNull() {
    // when
    eventPublisher.publishDeleteEvent(new AuthorityDto());

    // then
    verifyNoInteractions(eventProducer);
  }

  @Test
  void shouldSendDeletedEvent() {
    // given
    var dto = new AuthorityDto().id(UUID.randomUUID()).source("source");
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);

    // when
    eventPublisher.publishDeleteEvent(dto);

    // then
    verify(eventProducer).sendMessage(eq(dto.getId().toString()), captor.capture(), eq(DOMAIN_EVENT_TYPE_HEADER),
        eq(DomainEventType.DELETE));
    assertEquals(dto, captor.getValue().getOldEntity());
    assertEquals(TENANT_ID, captor.getValue().getTenant());
  }

  @Test
  void shouldNotSendReindexEventWhenIdIsNull() {
    // when
    var context = mock(ReindexContext.class);
    eventPublisher.publishReindexEvent(new AuthorityDto(), context);

    // then
    verifyNoInteractions(eventProducer);
  }

  @Test
  void shouldSendReindexEvent() {
    // given
    var id = UUID.randomUUID();
    var context = mock(ReindexContext.class);
    var dto = new AuthorityDto().id(id).source("source");
    when(context.getTenantId()).thenReturn(TENANT_ID);
    when(context.getJobId()).thenReturn(id);

    // when
    eventPublisher.publishReindexEvent(dto, context);

    // then
    verify(eventProducer).sendMessage(eq(dto.getId().toString()), captor.capture(),
        eq("reindex-job-id"), eq(id), eq(DOMAIN_EVENT_TYPE_HEADER), eq(DomainEventType.REINDEX));
    assertEquals(dto, captor.getValue().getNewEntity());
    assertEquals(TENANT_ID, captor.getValue().getTenant());
  }
}
