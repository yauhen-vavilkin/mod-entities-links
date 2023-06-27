package org.folio.entlinks.exception.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

  VALIDATION_ERROR("validation"),
  NOT_FOUND_ERROR("not-found"),
  INTEGRATION_ERROR("intregration-error"),
  UNKNOWN_ERROR("unknown");

  private final String value;
}
