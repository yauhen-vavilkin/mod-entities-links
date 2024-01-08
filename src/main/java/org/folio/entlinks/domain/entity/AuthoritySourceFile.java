package org.folio.entlinks.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.entlinks.domain.entity.base.Identifiable;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "authority_source_file", uniqueConstraints = {
  @UniqueConstraint(name = "uc_authoritysourcefile_base_url", columnNames = {"base_url"}),
  @UniqueConstraint(name = "uc_authoritysourcefile_name", columnNames = {"name"})
})
public class AuthoritySourceFile extends MetadataEntity implements Persistable<UUID>, Identifiable<UUID> {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "type", length = 100)
  private String type;

  @Column(name = "base_url", unique = true)
  private String baseUrl;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "source", length = 100)
  private AuthoritySourceFileSource source;

  @ToString.Exclude
  @OneToMany(mappedBy = "authoritySourceFile", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  private Set<AuthoritySourceFileCode> authoritySourceFileCodes = new LinkedHashSet<>();

  @ToString.Exclude
  @OneToMany(mappedBy = "authoritySourceFile", fetch = FetchType.LAZY)
  private Set<Authority> authorities = new LinkedHashSet<>();

  @Column(name = "sequence_name", length = 50)
  private String sequenceName;

  @Column(name = "selectable")
  private boolean selectable = false;

  @Column(name = "hrid_start_number")
  private Integer hridStartNumber;

  @Transient
  private boolean isNew = true;

  public AuthoritySourceFile(AuthoritySourceFile other) {
    super(other);
    this.id = other.id;
    this.name = other.name;
    this.source = other.source;
    this.type = other.type;
    this.baseUrl = other.baseUrl;
    this.authoritySourceFileCodes = Optional.ofNullable(other.authoritySourceFileCodes)
      .orElse(Set.of())
      .stream()
      .map(AuthoritySourceFileCode::new)
      .collect(java.util.stream.Collectors.toSet());
    this.authorities = Optional.ofNullable(other.authorities).orElse(Set.of()).stream()
        .map(Authority::new)
        .collect(Collectors.toSet());
    this.sequenceName = other.sequenceName;
    this.selectable = other.selectable;
    this.hridStartNumber = other.hridStartNumber;
  }

  public void addCode(AuthoritySourceFileCode code) {
    authoritySourceFileCodes.add(code);
    code.setAuthoritySourceFile(this);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    AuthoritySourceFile that = (AuthoritySourceFile) o;
    return id != null && Objects.equals(id, that.id);
  }

  @PostLoad
  @PrePersist
  void markNotNew() {
    this.isNew = false;
  }

  public void markAsConsortiumShadowCopy() {
    this.setSource(AuthoritySourceFileSource.CONSORTIUM);
  }

  public boolean isConsortiumShadowCopy() {
    return AuthoritySourceFileSource.CONSORTIUM.equals(this.getSource());
  }
}
