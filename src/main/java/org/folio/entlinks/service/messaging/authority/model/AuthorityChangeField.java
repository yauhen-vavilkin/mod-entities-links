package org.folio.entlinks.service.messaging.authority.model;

import lombok.Getter;

public enum AuthorityChangeField {

  PERSONAL_NAME("personalName", "100"),
  PERSONAL_NAME_TITLE("personalNameTitle", "100"),
  CORPORATE_NAME("corporateName", "110"),
  CORPORATE_NAME_TITLE("corporateNameTitle", "110"),
  MEETING_NAME("meetingName", "100"),
  MEETING_NAME_TITLE("meetingNameTitle", "100"),
  UNIFORM_TITLE("uniformTitle", "100"),
  TOPICAL_TERM("topicalTerm", "100"),
  GEOGRAPHIC_NAME("geographicName", "100"),
  GENRE_TERM("genreTerm", "100"),
  NATURAL_ID("naturalId", "010");

  @Getter
  private final String fieldName;
  @Getter
  private final String type;

  AuthorityChangeField(String fieldName, String type) {
    this.fieldName = fieldName;
    this.type = type;
  }

  public static AuthorityChangeField fromValue(String value) {
    for (AuthorityChangeField b : AuthorityChangeField.values()) {
      if (b.fieldName.equalsIgnoreCase(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
