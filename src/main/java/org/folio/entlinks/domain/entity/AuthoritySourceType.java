package org.folio.entlinks.domain.entity;

public enum AuthoritySourceType {
  FOLIO("folio"),

  LOCAL("local");

  private final String value;

  AuthoritySourceType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
