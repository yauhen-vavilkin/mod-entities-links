package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
@Table(name = "authority")
public class Authority extends MetadataEntity implements Persistable<UUID>, Identifiable<UUID> {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "natural_id")
  private String naturalId;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_file_id", nullable = false)
  private AuthoritySourceFile authoritySourceFile;

  @Column(name = "source")
  private String source;

  @Column(name = "heading")
  private String heading;

  @Column(name = "heading_type")
  private String headingType;

  @Version
  @Column(name = "_version", nullable = false)
  private int version;

  @Column(name = "subject_heading_code")
  private Character subjectHeadingCode;

  @Column(name = "sft_headings")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<HeadingRef> sftHeadings;

  @Column(name = "saft_headings")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<HeadingRef> saftHeadings;

  @Column(name = "identifiers")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<AuthorityIdentifier> identifiers;

  @Column(name = "notes")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<AuthorityNote> notes;

  @Transient
  private boolean isNew = true;

  public Authority(Authority other) {
    this.id = other.id;
    this.naturalId = other.naturalId;
    this.authoritySourceFile = new AuthoritySourceFile(other.authoritySourceFile);
    this.source = other.source;
    this.heading = other.heading;
    this.headingType = other.headingType;
    this.version = other.version;
    this.subjectHeadingCode = other.subjectHeadingCode;
    this.sftHeadings = Optional.ofNullable(other.getSftHeadings()).orElse(List.of()).stream()
        .map(HeadingRef::new)
        .toList();
    this.saftHeadings = Optional.ofNullable(other.getSaftHeadings()).orElse(List.of()).stream()
        .map(HeadingRef::new)
        .toList();
    this.identifiers = Optional.ofNullable(other.getIdentifiers()).orElse(List.of()).stream()
        .map(AuthorityIdentifier::new)
        .toList();
    this.notes = Optional.ofNullable(other.getNotes()).orElse(List.of()).stream()
        .map(AuthorityNote::new)
        .toList();
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
    Authority that = (Authority) o;
    return id != null && Objects.equals(id, that.id);
  }

  @PostLoad
  @PrePersist
  void markNotNew() {
    this.isNew = false;
  }
}
