package org.folio.entlinks.integration.kafka;

import static org.folio.entlinks.domain.dto.AuthorityEventType.DELETE;
import static org.folio.entlinks.domain.dto.AuthorityEventType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.entlinks.domain.dto.AuthorityEvent;
import org.folio.entlinks.domain.dto.AuthorityRecord;
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
  private ConsumerRecord<String, AuthorityEvent> consumerRecord;

  @Test
  void shouldNotFilterDeleteEvent() {
    var event = new AuthorityEvent().type(DELETE.getValue());
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertFalse(actual);
  }

  @Test
  void shouldNotFilterUpdateEvent_whenNewAndOldAreNotEqual() {
    var newRecord = new AuthorityRecord().naturalId("1");
    var oldRecord = new AuthorityRecord().naturalId("2");
    var event = new AuthorityEvent().type(UPDATE.getValue())._new(newRecord).old(oldRecord);
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertFalse(actual);
  }

  @Test
  void shouldFilterUpdateEvent_whenNewAndOldAreEqual() {
    var newRecord = new AuthorityRecord().naturalId("1");
    var oldRecord = new AuthorityRecord().naturalId("1");
    var event = new AuthorityEvent().type(UPDATE.getValue())._new(newRecord).old(oldRecord);
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertTrue(actual);
  }

  @ValueSource(strings = {"REINDEX", "ITERATE", "NEW"})
  @ParameterizedTest
  void shouldFilterEvent_whenTypeIsNotSupported(String type) {
    var event = new AuthorityEvent().type(type);
    mockConsumerRecord(event);

    var actual = filterStrategy.filter(consumerRecord);

    assertTrue(actual);
  }

  private void mockConsumerRecord(AuthorityEvent event) {
    when(consumerRecord.value()).thenReturn(event);
  }
}
