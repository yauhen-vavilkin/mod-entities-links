package org.folio.entlinks.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.qm.domain.dto.AuthorityInventoryRecord;
import org.folio.qm.domain.dto.InventoryEvent;
import org.folio.qm.domain.dto.InventoryEventType;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorityChangeFilterStrategyTest {

  private final AuthorityChangeFilterStrategy filterStrategy = new AuthorityChangeFilterStrategy();

  @Mock
  private ConsumerRecord<String, InventoryEvent> consumerRecord;

  @Test
  void shouldNotFilterDeleteEvent() {
    var event = new InventoryEvent().type(InventoryEventType.DELETE.getValue());
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertFalse(actual);
  }

  @Test
  void shouldNotFilterUpdateEvent_whenNewAndOldAreNotEqual() {
    var newRecord = new AuthorityInventoryRecord().naturalId("1");
    var oldRecord = new AuthorityInventoryRecord().naturalId("2");
    var event = new InventoryEvent()
      .type(InventoryEventType.UPDATE.getValue())
      ._new(newRecord).old(oldRecord);
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertFalse(actual);
  }

  @Test
  void shouldFilterUpdateEvent_whenNewAndOldAreEqual() {
    var newRecord = new AuthorityInventoryRecord().naturalId("1");
    var oldRecord = new AuthorityInventoryRecord().naturalId("1");
    var event = new InventoryEvent()
      .type(InventoryEventType.UPDATE.getValue())
      ._new(newRecord).old(oldRecord);
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertTrue(actual);
  }

  @ValueSource(strings = {"REINDEX", "ITERATE", "NEW"})
  @ParameterizedTest
  void shouldFilterEvent_whenTypeIsNotSupported(String type) {
    var event = new InventoryEvent().type(type);
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertTrue(actual);
  }

  private void mockConsumerRecord(InventoryEvent event) {
    when(consumerRecord.value()).thenReturn(event);
  }
}
