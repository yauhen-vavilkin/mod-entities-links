package org.folio.entlinks.domain.entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.entlinks.domain.entity.converter.StringToCharArrayConverter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

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
@TypeDef(name = "enum", typeClass = PostgreSQLEnumType.class)
public class InstanceAuthorityLink {

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

  @Type(type = "enum")
  @Column(name = "status", nullable = false)
  private InstanceAuthorityLinkStatus status;

  @Column(name = "error_cause")
  private String errorCause;

  @CreatedDate
  @Column(name = "created_at")
  private Timestamp createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private Timestamp updatedAt;

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
