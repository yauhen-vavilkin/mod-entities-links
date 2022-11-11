package org.folio.entlinks;

public enum LinkingPairType {
  INSTANCE_AUTHORITY("instance-authority");

  private final String value;

  LinkingPairType(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
