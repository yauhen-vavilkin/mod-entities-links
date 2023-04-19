package org.folio.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonTestUtils {

  @SneakyThrows
  public static String asJson(Object value, ObjectMapper mapper) {
    return mapper.writeValueAsString(value);
  }

  @SneakyThrows
  public static <T> T toObject(String json, TypeReference<T> type, ObjectMapper mapper) {
    return mapper.readValue(json, type);
  }
}
