package org.folio.entlinks.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
import org.hibernate.Hibernate;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "authority_source_file", uniqueConstraints = {
  @UniqueConstraint(name = "uc_authoritysourcefile_base_url", columnNames = {"base_url"}),
  @UniqueConstraint(name = "uc_authoritysourcefile_name", columnNames = {"name"})
})
public class AuthoritySourceFile extends MetadataEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "type", length = 100)
  private String type;

  @Column(name = "base_url", unique = true)
  private String baseUrl;

  @Column(name = "source", length = 100)
  private String source;

  @OneToMany(mappedBy = "authoritySourceFile", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  private Set<AuthoritySourceFileCode> authoritySourceFileCodes = new LinkedHashSet<>();

  @OneToMany(mappedBy = "authoritySourceFile", fetch = FetchType.LAZY)
  private Set<Authority> authorities = new LinkedHashSet<>();

  public AuthoritySourceFile(AuthoritySourceFile other) {
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
  }

  public void addCode(AuthoritySourceFileCode code) {
    authoritySourceFileCodes.add(code);
    code.setAuthoritySourceFile(this);
  }

  public void removeCode(AuthoritySourceFileCode code) {
    authoritySourceFileCodes.remove(code);
    code.setAuthoritySourceFile(null);
  }

  public void addAuthorityStorage(Authority authority) {
    authorities.add(authority);
    authority.setAuthoritySourceFile(this);
  }

  public void removeAuthorityStorage(Authority authority) {
    authorities.remove(authority);
    authority.setAuthoritySourceFile(null);
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
}
