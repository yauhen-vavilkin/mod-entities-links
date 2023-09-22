package org.folio.entlinks.service.messaging.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.integration.dto.AuthorityDomainEvent;
import org.folio.entlinks.integration.kafka.EventProducer;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.entlinks.service.messaging.authority.handler.AuthorityChangeHandler;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.folio.entlinks.service.reindex.event.DomainEventType;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityLinkUpdateServiceTest {

  private @Captor ArgumentCaptor<List<LinksChangeEvent>> argumentCaptor;

  private @Mock EventProducer<LinksChangeEvent> eventProducer;
  private @Mock AuthorityDataStatService authorityDataStatService;

  private @Mock AuthorityChangeHandler updateHandler;
  private @Mock AuthorityChangeHandler deleteHandler;
  private @Mock AuthorityMappingRulesProcessingService mappingRulesProcessingService;
  private @Mock InstanceAuthorityLinkingService linkingService;

  private InstanceAuthorityLinkUpdateService service;

  @BeforeEach
  void setUp() {
    when(updateHandler.supportedAuthorityChangeType()).thenReturn(AuthorityChangeType.UPDATE);
    when(deleteHandler.supportedAuthorityChangeType()).thenReturn(AuthorityChangeType.DELETE);

    service = new InstanceAuthorityLinkUpdateService(authorityDataStatService,
      mappingRulesProcessingService, linkingService, eventProducer, List.of(updateHandler, deleteHandler));
  }

  @Test
  void handleAuthoritiesChanges_positive_updateEvent() {
    final var id = UUID.randomUUID();
    final var authorityEvents = List.of(
      new AuthorityDomainEvent(id, null, new AuthorityDto().naturalId("new"), DomainEventType.UPDATE, TENANT_ID));

    var expected = new LinksChangeEvent().type(LinksChangeEvent.TypeEnum.UPDATE);
    when(linkingService.countLinksByAuthorityIds(Set.of(id))).thenReturn(Map.of(id, 1));
    when(updateHandler.handle(anyList())).thenReturn(List.of(expected));

    service.handleAuthoritiesChanges(authorityEvents);

    verify(eventProducer).sendMessages(argumentCaptor.capture());
    verify(authorityDataStatService).createInBatch(anyList());
    verifyNoMoreInteractions(authorityDataStatService);

    var messages = argumentCaptor.getValue();
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getType()).isEqualTo(LinksChangeEvent.TypeEnum.UPDATE);
  }

  @Test
  void handleAuthoritiesChanges_positive_updateEventWhenNoLinksExist() {
    final var id = UUID.randomUUID();
    final var authorityEvents = List.of(
      new AuthorityDomainEvent(id, null, new AuthorityDto().naturalId("new"), DomainEventType.UPDATE, TENANT_ID));

    when(linkingService.countLinksByAuthorityIds(Set.of(id))).thenReturn(Collections.emptyMap());

    service.handleAuthoritiesChanges(authorityEvents);

    verify(eventProducer, never()).sendMessages(argumentCaptor.capture());
    verify(authorityDataStatService).createInBatch(anyList());
    verifyNoMoreInteractions(authorityDataStatService);
  }

  @Test
  void handleAuthoritiesChanges_positive_deleteEvent() {
    final var id = UUID.randomUUID();
    final var authorityEvents = List.of(
      new AuthorityDomainEvent(id, new AuthorityDto().naturalId("old"), null, DomainEventType.DELETE, TENANT_ID));

    var changeEvent = new LinksChangeEvent().type(LinksChangeEvent.TypeEnum.DELETE);

    when(linkingService.countLinksByAuthorityIds(Set.of(id))).thenReturn(Map.of(id, 1));
    when(deleteHandler.handle(any())).thenReturn(List.of(changeEvent));

    service.handleAuthoritiesChanges(authorityEvents);

    verify(eventProducer).sendMessages(argumentCaptor.capture());
    verify(authorityDataStatService).createInBatch(anyList());

    var messages = argumentCaptor.getValue();
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getType()).isEqualTo(LinksChangeEvent.TypeEnum.DELETE);
  }
}
