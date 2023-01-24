package org.folio.entlinks.domain.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter for converting from char[] to String and vice versa.
 */
@Converter(autoApply = true)
public class StringToCharArrayConverter implements AttributeConverter<char[], String> {

  @Override
  public String convertToDatabaseColumn(char[] attribute) {
    return attribute == null ? "" : String.valueOf(attribute);
  }

  @Override
  public char[] convertToEntityAttribute(String dbData) {
    return dbData == null || dbData.isBlank() ? null : dbData.toCharArray();
  }
}
