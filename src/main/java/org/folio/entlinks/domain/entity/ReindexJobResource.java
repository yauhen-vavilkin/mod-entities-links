package org.folio.entlinks.domain.entity;

import lombok.Getter;

public enum ReindexJobResource {

  AUTHORITY("authority");

  private final @Getter String authority;

  ReindexJobResource(String authority) {
    this.authority = authority;
  }
}
