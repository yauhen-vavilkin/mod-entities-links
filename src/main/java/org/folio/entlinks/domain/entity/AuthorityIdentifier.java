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
public class AuthorityIdentifier implements Serializable {

  private String value;

  private UUID identifierTypeId;

  public AuthorityIdentifier(AuthorityIdentifier other) {
    this.identifierTypeId = other.identifierTypeId;
    this.value = other.value;
  }
}
