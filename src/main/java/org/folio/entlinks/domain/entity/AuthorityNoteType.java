package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.folio.entlinks.domain.entity.base.Identifiable;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Entity
@Table(name = "authority_note_type", uniqueConstraints = {
  @UniqueConstraint(name = "uc_authoritynotetype_name", columnNames = {"name"})
})
public class AuthorityNoteType extends MetadataEntity implements Persistable<UUID>, Identifiable<UUID> {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "source", length = 100)
  private String source;

  @Transient
  private boolean isNew = true;

  @PostLoad
  @PrePersist
  void markNotNew() {
    this.isNew = false;
  }
}
