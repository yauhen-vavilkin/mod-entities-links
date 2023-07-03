package org.folio.entlinks.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "authority_note_type_metadata", uniqueConstraints = {
  @UniqueConstraint(name = "uc_authority_note_type_metadata", columnNames = {"authority_note_type_id"})
})
public class AuthorityNoteTypeMetadata extends MetadataEntity {

  @OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
  @JoinColumn(name = "authority_note_type_id", nullable = false, unique = true)
  private AuthorityNoteType authorityNoteType;

}
