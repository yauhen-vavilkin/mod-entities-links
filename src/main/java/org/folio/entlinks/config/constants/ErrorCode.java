package org.folio.entlinks.config.constants;

import lombok.Getter;

public enum ErrorCode {
  NO_SUGGESTIONS("101", ""),
  MORE_THAN_ONE_SUGGESTIONS("102", ""),
  DISABLED_AUTO_LINKING("103", ""),
  DUPLICATE_AUTHORITY_SOURCE_FILE_NAME("104", "Authority source file with the given 'name' already exists."),
  DUPLICATE_AUTHORITY_SOURCE_FILE_URL("105", "Authority source file with the given 'baseUrl' already exists."),
  DUPLICATE_AUTHORITY_SOURCE_FILE_CODE("106", "Authority source file with the given 'code' already exists."),
  DUPLICATE_NOTE_TYPE_NAME("107", "Authority note type with the given 'name' already exists."),
  VIOLATION_OF_RELATION_BETWEEN_AUTHORITY_AND_SOURCE_FILE("108",
      "Cannot complete operation on the entity due to it's relation with Authority/Authority Source File.");

  @Getter
  private final String errorCode;
  @Getter
  private final String message;

  ErrorCode(String errorCode, String message) {
    this.errorCode = errorCode;
    this.message = message;
  }
}
