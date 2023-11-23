package org.folio.entlinks.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.entity.base.Identifiable;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "authority")
public class Authority extends AuthorityBase implements Persistable<UUID>, Identifiable<UUID> {

  private static final String CONSORTIUM_SOURCE_PREFIX = "CONSORTIUM-";

  @Transient
  private boolean isNew = true;

  public Authority(Authority other) {
    super(other);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
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
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @PostLoad
  @PrePersist
  void markNotNew() {
    this.isNew = false;
  }

  public void makeAsConsortiumShadowCopy() {
    this.setSource(StringUtils.prependIfMissing(this.getSource(), CONSORTIUM_SOURCE_PREFIX));
  }

  public boolean isConsortiumShadowCopy() {
    return this.getSource() != null && this.getSource().startsWith(CONSORTIUM_SOURCE_PREFIX);
  }

  public static AuthorityBuilder builder() {
    return new AuthorityBuilder();
  }

  public static class AuthorityBuilder {
    private UUID id;
    private String naturalId;
    private AuthoritySourceFile authoritySourceFile;
    private String source;
    private String heading;
    private String headingType;
    private int version;
    private Character subjectHeadingCode;
    private List<HeadingRef> sftHeadings;
    private List<HeadingRef> saftHeadings;
    private List<AuthorityIdentifier> identifiers;
    private List<AuthorityNote> notes;
    private boolean deleted;
    private Timestamp updatedDate;
    private Timestamp createdDate;
    private UUID createdByUserId;
    private UUID updatedByUserId;

    AuthorityBuilder() {
    }

    public AuthorityBuilder id(UUID id) {
      this.id = id;
      return this;
    }

    public AuthorityBuilder naturalId(String naturalId) {
      this.naturalId = naturalId;
      return this;
    }

    public AuthorityBuilder authoritySourceFile(AuthoritySourceFile authoritySourceFile) {
      this.authoritySourceFile = authoritySourceFile;
      return this;
    }

    public AuthorityBuilder source(String source) {
      this.source = source;
      return this;
    }

    public AuthorityBuilder heading(String heading) {
      this.heading = heading;
      return this;
    }

    public AuthorityBuilder headingType(String headingType) {
      this.headingType = headingType;
      return this;
    }

    public AuthorityBuilder version(int version) {
      this.version = version;
      return this;
    }

    public AuthorityBuilder subjectHeadingCode(Character subjectHeadingCode) {
      this.subjectHeadingCode = subjectHeadingCode;
      return this;
    }

    public AuthorityBuilder sftHeadings(List<HeadingRef> sftHeadings) {
      this.sftHeadings = sftHeadings;
      return this;
    }

    public AuthorityBuilder saftHeadings(List<HeadingRef> saftHeadings) {
      this.saftHeadings = saftHeadings;
      return this;
    }

    public AuthorityBuilder identifiers(List<AuthorityIdentifier> identifiers) {
      this.identifiers = identifiers;
      return this;
    }

    public AuthorityBuilder notes(List<AuthorityNote> notes) {
      this.notes = notes;
      return this;
    }

    public AuthorityBuilder deleted(boolean deleted) {
      this.deleted = deleted;
      return this;
    }

    public AuthorityBuilder updatedDate(Timestamp updatedDate) {
      this.updatedDate = updatedDate;
      return this;
    }

    public AuthorityBuilder createdDate(Timestamp createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public AuthorityBuilder updatedByUserId(UUID updatedBy) {
      this.updatedByUserId = updatedBy;
      return this;
    }

    public AuthorityBuilder createdByUserId(UUID createdBy) {
      this.createdByUserId = createdBy;
      return this;
    }

    public Authority build() {
      var authority = new Authority();
      authority.setId(id);
      authority.setNaturalId(naturalId);
      authority.setAuthoritySourceFile(authoritySourceFile);
      authority.setSource(source);
      authority.setHeading(heading);
      authority.setHeadingType(headingType);
      authority.setVersion(version);
      authority.setSubjectHeadingCode(subjectHeadingCode);
      authority.setSftHeadings(sftHeadings);
      authority.setSaftHeadings(saftHeadings);
      authority.setIdentifiers(identifiers);
      authority.setNotes(notes);
      authority.setDeleted(deleted);
      authority.setUpdatedDate(updatedDate);
      authority.setCreatedDate(createdDate);
      authority.setUpdatedByUserId(updatedByUserId);
      authority.setCreatedByUserId(createdByUserId);
      return authority;
    }

    public String toString() {
      return "Authority.AuthorityBuilder("
          + "id=" + this.id
          + ", naturalId=" + this.naturalId
          + ", authoritySourceFile=" + this.authoritySourceFile
          + ", source=" + this.source
          + ", heading=" + this.heading
          + ", headingType=" + this.headingType
          + ", version=" + this.version
          + ", subjectHeadingCode=" + this.subjectHeadingCode
          + ", sftHeadings=" + this.sftHeadings
          + ", saftHeadings=" + this.saftHeadings
          + ", identifiers=" + this.identifiers
          + ", notes=" + this.notes
          + ", deleted=" + this.deleted
          + ")";
    }
  }
}
