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
@Table(name = "authority_source_file_metadata", uniqueConstraints = {
  @UniqueConstraint(name = "uc_authority_source_file_metadata", columnNames = {"authority_source_file_id"})
})
public class AuthoritySourceFileMetadata extends MetadataEntity {

  @OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
  @JoinColumn(name = "authority_source_file_id", nullable = false, unique = true)
  private AuthoritySourceFile authoritySourceFile;

}
