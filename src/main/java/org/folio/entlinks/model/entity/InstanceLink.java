package org.folio.entlinks.model.entity;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "instance_link", indexes = {
  @Index(name = "idx_instancelink_authority_id", columnList = "authority_id"),
  @Index(name = "idx_instancelink_instance_id", columnList = "instance_id")
})
@TypeDef(name = "list-array", typeClass = ListArrayType.class)
public class InstanceLink {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @NotNull
  @Column(name = "authority_id", nullable = false)
  private UUID authorityId;

  @NotNull
  @Column(name = "authority_natural_id", nullable = false, length = 100)
  private String authorityNaturalId;

  @NotNull
  @Column(name = "instance_id", nullable = false)
  private UUID instanceId;

  @Column(name = "bib_record_tag", length = 3)
  private String bibRecordTag;

  @Type(type = "list-array")
  @Column(name = "bib_record_subfields")
  private List<String> bibRecordSubfields;

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
    InstanceLink instanceLink = (InstanceLink) o;
    return id != null && Objects.equals(id, instanceLink.id);
  }

  public boolean isSameLink(InstanceLink link) {
    return authorityId.equals(link.authorityId)
      && instanceId.equals(link.instanceId)
      && bibRecordTag.equals(link.bibRecordTag);
  }
}
