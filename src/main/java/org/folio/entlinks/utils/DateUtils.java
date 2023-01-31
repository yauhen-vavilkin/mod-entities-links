package org.folio.entlinks.utils;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DateUtils {

  public static OffsetDateTime fromTimestamp(Timestamp value) {
    return value != null ? OffsetDateTime.from(value.toInstant().atZone(ZoneId.systemDefault())) : null;
  }

  public static Timestamp toTimestamp(OffsetDateTime value) {
    return value != null ? Timestamp.valueOf(value.toLocalDateTime()) : null;
  }
}
