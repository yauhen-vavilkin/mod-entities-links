package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.converter.StringToCharArrayConverter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "instance_authority_linking_rule", uniqueConstraints = {
  @UniqueConstraint(name = "unq_bib_field_authority_fields", columnNames = {"bib_field", "authority_field"})
})
public class InstanceAuthorityLinkingRule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @Column(name = "bib_field", nullable = false, length = 3)
  private String bibField;

  @Column(name = "authority_field", nullable = false, length = 3)
  private String authorityField;

  @Convert(converter = StringToCharArrayConverter.class)
  @Column(name = "authority_subfields", nullable = false, length = 30)
  private char[] authoritySubfields;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "subfield_modifications", columnDefinition = "jsonb")
  private List<SubfieldModification> subfieldModifications;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "subfields_existence_validations", columnDefinition = "jsonb")
  private Map<String, Boolean> subfieldsExistenceValidations;

  @Column(name = "auto_linking_enabled", nullable = false)
  private Boolean autoLinkingEnabled;

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    InstanceAuthorityLinkingRule that = (InstanceAuthorityLinkingRule) o;
    return id != null && Objects.equals(id, that.id);
  }
}
