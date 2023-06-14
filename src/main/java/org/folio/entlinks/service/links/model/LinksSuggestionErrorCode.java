package org.folio.entlinks.service.links.model;

import lombok.Getter;

public enum LinksSuggestionErrorCode {
  NO_SUGGESTIONS("101"),
  MORE_THEN_ONE_SUGGESTIONS("102"),
  DISABLED_AUTO_LINKING("103");

  @Getter
  private final String errorCode;

  LinksSuggestionErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }
}
