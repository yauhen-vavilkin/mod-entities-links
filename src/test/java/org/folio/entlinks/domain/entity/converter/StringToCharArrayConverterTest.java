package org.folio.entlinks.domain.entity.converter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class StringToCharArrayConverterTest {

  private static final char[] CHARS_VALUE = {'f', '0', '1', 'i', '0'};
  private static final String STRING_VALUE = "f01i0";

  private final StringToCharArrayConverter converter = new StringToCharArrayConverter();

  @Test
  void convertToDatabaseColumn() {
    var actual = converter.convertToDatabaseColumn(CHARS_VALUE);
    assertEquals(STRING_VALUE, actual);
  }

  @Test
  void convertToDatabaseColumn_whenArrayIsNull() {
    var actual = converter.convertToDatabaseColumn(null);
    assertEquals("", actual);
  }

  @Test
  void convertToDatabaseColumn_whenArrayIsEmpty() {
    var actual = converter.convertToDatabaseColumn(new char[0]);
    assertEquals("", actual);
  }

  @Test
  void convertToEntityAttribute() {
    var actual = converter.convertToEntityAttribute(STRING_VALUE);
    assertArrayEquals(CHARS_VALUE, actual);
  }

  @Test
  void convertToEntityAttribute_whenStringIsNullOrBlank() {
    var actual = converter.convertToEntityAttribute(null);
    assertArrayEquals(null, actual);
  }

  @ValueSource(strings = {"", "  "})
  @ParameterizedTest
  void convertToEntityAttribute_whenStringIsEmptyOrBlank(String dbData) {
    var actual = converter.convertToEntityAttribute(dbData);
    assertArrayEquals(null, actual);
  }

}
