package org.folio.entlinks.domain.entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.entlinks.domain.dto.SubfieldModification;
import org.folio.entlinks.domain.entity.converter.StringToCharArrayConverter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

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
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@TypeDef(name = "enum", typeClass = PostgreSQLEnumType.class)
public class InstanceAuthorityLinkingRule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "bib_field", nullable = false, length = 3)
  private String bibField;

  @Column(name = "authority_field", nullable = false, length = 3)
  private String authorityField;

  @Convert(converter = StringToCharArrayConverter.class)
  @Column(name = "authority_subfields", nullable = false, length = 30)
  private char[] authoritySubfields;

  @Type(type = "jsonb")
  @Column(name = "subfieldModifications", columnDefinition = "jsonb")
  private List<SubfieldModification> subfieldModifications;

  @Type(type = "jsonb")
  @Column(name = "subfieldsExistenceValidations", columnDefinition = "jsonb")
  private Map<String, Boolean> subfieldsExistenceValidations;

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
