package org.folio.entlinks.integration.kafka;

import static org.folio.entlinks.integration.dto.event.DomainEventType.DELETE;
import static org.folio.entlinks.integration.dto.event.DomainEventType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.integration.dto.event.AuthorityDomainEvent;
import org.folio.entlinks.integration.dto.event.DomainEventType;
import org.folio.spring.testing.type.UnitTest;
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
  private ConsumerRecord<String, AuthorityDomainEvent> consumerRecord;

  @Test
  void shouldNotFilterDeleteEvent() {
    var event = new AuthorityDomainEvent(null, null, null, DELETE, null, null);
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertFalse(actual);
  }

  @Test
  void shouldNotFilterUpdateEvent_whenNewAndOldAreNotEqual() {
    var newRecord = new AuthorityDto().naturalId("1");
    var oldRecord = new AuthorityDto().naturalId("2");
    var event = new AuthorityDomainEvent(null, oldRecord, newRecord, UPDATE, null, null);
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertFalse(actual);
  }

  @Test
  void shouldFilterUpdateEvent_whenNewAndOldAreEqual() {
    var newRecord = new AuthorityDto().naturalId("1");
    var oldRecord = new AuthorityDto().naturalId("1");
    var event = new AuthorityDomainEvent(null, oldRecord, newRecord, UPDATE, null, null);
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertTrue(actual);
  }

  @ValueSource(strings = {"REINDEX", "CREATE"})
  @ParameterizedTest
  void shouldFilterEvent_whenTypeIsNotSupported(String type) {
    var event = new AuthorityDomainEvent(null, null, null, DomainEventType.valueOf(type), null);
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertTrue(actual);
  }

  private void mockConsumerRecord(AuthorityDomainEvent event) {
    when(consumerRecord.value()).thenReturn(event);
  }
}
