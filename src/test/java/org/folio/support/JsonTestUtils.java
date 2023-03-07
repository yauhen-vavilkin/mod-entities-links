package org.folio.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonTestUtils {

  @SneakyThrows
  public static String asJson(Object value, ObjectMapper mapper) {
    return mapper.writeValueAsString(value);
  }
}
