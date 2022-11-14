package org.folio.entlinks.model.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@TypeDef(name = "json", typeClass = JsonType.class)
@Table(name = "linking_rules")
public class LinkingRules {

  @Id
  @Column(name = "linking_pair_type", unique = true)
  private String linkingPairType;

  @Type(type = "json")
  @Column(name = "jsonb", columnDefinition = "jsonb", nullable = false)
  private String jsonb;

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) { return false; }
    LinkingRules instanceLink = (LinkingRules) o;
    return Objects.equals(linkingPairType, instanceLink.linkingPairType)
        && Objects.equals(jsonb, instanceLink.jsonb);
  }
}
