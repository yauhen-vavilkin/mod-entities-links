package org.folio.entlinks.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.apache.kafka.common.header.Header;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class KafkaUtilsTest {

  @Test
  void getTenantTopicName_positive() {
    var actual = KafkaUtils.getTenantTopicName("topic-name", "tenant");

    assertEquals("folio.tenant.topic-name", actual);
  }

  @Test
  void toKafkaHeaders_positive() {
    var actual = KafkaUtils.toKafkaHeaders(Map.of(
      "tenant", List.of("tId"),
      "token", List.of("secured")
    ));

    assertThat(actual)
      .hasSize(2)
      .extracting(Header::key, Header::value)
      .contains(tuple("tenant", "tId".getBytes()), tuple("token", "secured".getBytes()));
  }

  @Test
  void toKafkaHeaders_positive_empty() {
    var actual = KafkaUtils.toKafkaHeaders(Map.of());

    assertThat(actual).isEmpty();
  }
}
