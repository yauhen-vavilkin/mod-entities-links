package org.folio.entlinks.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;
import java.time.Instant;
import lombok.SneakyThrows;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class DateUtilsTest {

  @Test
  @SneakyThrows
  void createDate() {
    var dateFromTimestamp = DateUtils.fromTimestamp(Timestamp.from(Instant.now()));
    var timestamp = DateUtils.toTimestamp(dateFromTimestamp);
    assertNotNull(dateFromTimestamp);
    assertNotNull(timestamp);
  }

  @Test
  @SneakyThrows
  void createDateFromNull() {
    var dateFromTimestamp = DateUtils.fromTimestamp(null);
    var timestamp = DateUtils.toTimestamp(null);
    assertNull(dateFromTimestamp);
    assertNull(timestamp);
  }
}
