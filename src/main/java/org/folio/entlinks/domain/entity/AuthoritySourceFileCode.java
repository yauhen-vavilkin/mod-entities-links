package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
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
@Table(name = "authority_source_file_code", uniqueConstraints = {
  @UniqueConstraint(name = "uc_authoritysourcefilecode_code", columnNames = {"code"})
})
public class AuthoritySourceFileCode {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "authority_source_file_code_id_seq")
  @Column(name = "id", nullable = false)
  private Integer id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "authority_source_file_id", nullable = false)
  private AuthoritySourceFile authoritySourceFile;

  @Column(name = "code", nullable = false, unique = true, length = 25)
  private String code;

  public AuthoritySourceFileCode(AuthoritySourceFileCode other) {
    this.id = other.id;
    this.authoritySourceFile = other.authoritySourceFile;
    this.code = other.code;
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
    AuthoritySourceFileCode that = (AuthoritySourceFileCode) o;
    return id != null && Objects.equals(id, that.id);
  }
}
