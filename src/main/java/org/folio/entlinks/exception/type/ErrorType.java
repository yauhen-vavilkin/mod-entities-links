package org.folio.entlinks.exception.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

  VALIDATION_ERROR("validation"),
  NOT_FOUND_ERROR("not-found"),
  INTEGRATION_ERROR("integration-error"),
  OPTIMISTIC_LOCKING_ERROR("optimistic locking"),
  UNKNOWN_ERROR("unknown");

  private final String value;
}
