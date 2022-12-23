package org.folio.entlinks.service.messaging.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.assertj.core.groups.Tuple;
import org.folio.entlinks.domain.dto.InventoryEvent;
import org.folio.entlinks.domain.dto.LinksChangeEvent;
import org.folio.entlinks.service.messaging.authority.handler.AuthorityChangeHandler;
import org.folio.entlinks.service.messaging.authority.model.AuthorityChangeType;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InstanceAuthorityLinkUpdateServiceTest {

  private static final String TENANT = "test-tenant";

  private @Captor ArgumentCaptor<ProducerRecord<String, LinksChangeEvent>> producerRecordCaptor;

  private @Mock FolioExecutionContext context;
  private @Mock KafkaTemplate<String, LinksChangeEvent> kafkaTemplate;

  private @Mock AuthorityChangeHandler updateHandler;
  private @Mock AuthorityChangeHandler deleteHandler;

  private InstanceAuthorityLinkUpdateService service;

  @BeforeEach
  void setUp() {
    when(updateHandler.supportedAuthorityChangeType()).thenReturn(AuthorityChangeType.UPDATE);
    when(deleteHandler.supportedAuthorityChangeType()).thenReturn(AuthorityChangeType.DELETE);
    when(context.getTenantId()).thenReturn(TENANT);

    service = new InstanceAuthorityLinkUpdateService(context, kafkaTemplate,
      List.of(updateHandler, deleteHandler));
  }

  @Test
  void handleAuthoritiesChanges_positive_updateEvent() {
    var inventoryEvents = List.of(new InventoryEvent().type("UPDATE"));

    var expected = new LinksChangeEvent().type(LinksChangeEvent.TypeEnum.UPDATE);

    when(updateHandler.handle(anyList())).thenReturn(List.of(expected));
    when(context.getOkapiHeaders()).thenReturn(Map.of("tenant", List.of(TENANT)));

    service.handleAuthoritiesChanges(inventoryEvents);

    verify(kafkaTemplate).send(producerRecordCaptor.capture());

    var producerRecord = producerRecordCaptor.getValue();
    assertThat(producerRecord).isNotNull();
    assertThat(producerRecord.headers())
      .extracting(Header::key, Header::value)
      .contains(Tuple.tuple("tenant", TENANT.getBytes()));

    assertThat(producerRecord.value())
      .extracting(LinksChangeEvent::getType, LinksChangeEvent::getTenant)
      .contains(LinksChangeEvent.TypeEnum.UPDATE, TENANT);
  }

  @Test
  void handleAuthoritiesChanges_positive_deleteEvent() {
    var inventoryEvents = List.of(new InventoryEvent().type("DELETE"));

    var changeEvent = new LinksChangeEvent().type(LinksChangeEvent.TypeEnum.DELETE);

    when(deleteHandler.handle(any())).thenReturn(List.of(changeEvent));
    when(context.getOkapiHeaders()).thenReturn(Map.of("tenant", List.of(TENANT)));

    service.handleAuthoritiesChanges(inventoryEvents);

    verify(kafkaTemplate).send(producerRecordCaptor.capture());

    var producerRecord = producerRecordCaptor.getValue();
    assertThat(producerRecord).isNotNull();
    assertThat(producerRecord.headers())
      .extracting(Header::key, Header::value)
      .contains(Tuple.tuple("tenant", TENANT.getBytes()));

    assertThat(producerRecord.value())
      .extracting(LinksChangeEvent::getType, LinksChangeEvent::getTenant)
      .contains(LinksChangeEvent.TypeEnum.DELETE, TENANT);
  }
}
