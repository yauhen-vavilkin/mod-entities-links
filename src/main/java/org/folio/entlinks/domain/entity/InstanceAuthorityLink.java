package org.folio.entlinks.domain.entity;

import io.hypersistence.utils.hibernate.type.basic.PostgreSQLEnumType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.entlinks.domain.entity.converter.StringToCharArrayConverter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;

@Entity
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "instance_authority_link", indexes = {
  @Index(name = "idx_instance_authority_link_authority_id", columnList = "authority_id"),
  @Index(name = "idx_instance_authority_link_instance_id", columnList = "instance_id")
})
public class InstanceAuthorityLink extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ToString.Exclude
  @ManyToOne(optional = false, cascade = {CascadeType.ALL})
  @JoinColumn(name = "authority_id", nullable = false)
  private AuthorityData authorityData;

  @NotNull
  @Column(name = "instance_id", nullable = false)
  private UUID instanceId;

  @Column(name = "bib_record_tag", length = 3)
  private String bibRecordTag;

  @Column(name = "bib_record_subfields", length = 30)
  @Convert(converter = StringToCharArrayConverter.class)
  private char[] bibRecordSubfields;

  @Column(name = "linking_rule_id", nullable = false)
  private Long linkingRuleId;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Type(PostgreSQLEnumType.class)
  @Column(name = "status", nullable = false)
  private InstanceAuthorityLinkStatus status = InstanceAuthorityLinkStatus.ACTUAL;

  @Column(name = "error_cause")
  private String errorCause;

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
    InstanceAuthorityLink instanceLink = (InstanceAuthorityLink) o;
    return id != null && Objects.equals(id, instanceLink.id);
  }

  public boolean isSameLink(InstanceAuthorityLink link) {
    return authorityData.getId().equals(link.getAuthorityData().getId())
      && instanceId.equals(link.instanceId)
      && bibRecordTag.equals(link.bibRecordTag);
  }
}
