package org.folio.entlinks.domain.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthorityNote implements Serializable {

  private UUID noteTypeId;

  private String note;

  private Boolean staffOnly;

  public AuthorityNote(AuthorityNote other) {
    this.noteTypeId = other.noteTypeId;
    this.note = other.note;
    this.staffOnly = other.staffOnly;
  }
}
