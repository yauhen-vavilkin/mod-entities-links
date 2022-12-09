package org.folio.entlinks.service.messaging.authority.model;

import lombok.Getter;

public enum AuthorityChange {

  PERSONAL_NAME("personalName"),
  PERSONAL_NAME_TITLE("personalNameTitle"),
  CORPORATE_NAME("corporateName"),
  CORPORATE_NAME_TITLE("corporateNameTitle"),
  MEETING_NAME("meetingName"),
  MEETING_NAME_TITLE("meetingNameTitle"),
  UNIFORM_TITLE("uniformTitle"),
  TOPICAL_TERM("topicalTerm"),
  GEOGRAPHIC_NAME("geographicName"),
  GENRE_TERM("genreTerm"),
  NATURAL_ID("naturalId");

  @Getter
  private final String fieldName;

  AuthorityChange(String fieldName) {
    this.fieldName = fieldName;
  }

  public static AuthorityChange fromValue(String value) {
    for (AuthorityChange b : AuthorityChange.values()) {
      if (b.fieldName.equalsIgnoreCase(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
