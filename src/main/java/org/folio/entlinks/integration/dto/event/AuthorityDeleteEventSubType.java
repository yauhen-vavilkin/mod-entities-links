package org.folio.entlinks.integration.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AuthorityDeleteEventSubType {

  SOFT_DELETE("SOFT_DELETE"),
  HARD_DELETE("HARD_DELETE");

  private final String value;

  AuthorityDeleteEventSubType(String type) {
    this.value = type;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static AuthorityDeleteEventSubType fromValue(String value) {
    for (AuthorityDeleteEventSubType b : AuthorityDeleteEventSubType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
