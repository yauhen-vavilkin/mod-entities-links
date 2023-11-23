package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class AuthorityBase extends MetadataEntity {
  public static final String ID_COLUMN = "id";
  public static final String NATURAL_ID_COLUMN = "natural_id";
  public static final String SOURCE_FILE_COLUMN = "source_file_id";
  public static final String SOURCE_COLUMN = "source";
  public static final String HEADING_COLUMN = "heading";
  public static final String HEADING_TYPE_COLUMN = "heading_type";
  public static final String VERSION_COLUMN = "_version";
  public static final String SUBJECT_HEADING_CODE_COLUMN = "subject_heading_code";
  public static final String SFT_HEADINGS_COLUMN = "sft_headings";
  public static final String SAFT_HEADINGS_COLUMN = "saft_headings";
  public static final String IDENTIFIERS_COLUMN = "identifiers";
  public static final String NOTES_COLUMN = "notes";
  public static final String DELETED_COLUMN = "deleted";

  @Id
  @Column(name = ID_COLUMN, nullable = false)
  private UUID id;

  @Column(name = NATURAL_ID_COLUMN)
  private String naturalId;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = SOURCE_FILE_COLUMN, nullable = false)
  private AuthoritySourceFile authoritySourceFile;

  @Column(name = SOURCE_COLUMN)
  private String source;

  @Column(name = HEADING_COLUMN)
  private String heading;

  @Column(name = HEADING_TYPE_COLUMN)
  private String headingType;

  @Version
  @Column(name = VERSION_COLUMN, nullable = false)
  private int version;

  @Column(name = SUBJECT_HEADING_CODE_COLUMN)
  private Character subjectHeadingCode;

  @Column(name = SFT_HEADINGS_COLUMN)
  @JdbcTypeCode(SqlTypes.JSON)
  private List<HeadingRef> sftHeadings;

  @Column(name = SAFT_HEADINGS_COLUMN)
  @JdbcTypeCode(SqlTypes.JSON)
  private List<HeadingRef> saftHeadings;

  @Column(name = IDENTIFIERS_COLUMN)
  @JdbcTypeCode(SqlTypes.JSON)
  private List<AuthorityIdentifier> identifiers;

  @Column(name = NOTES_COLUMN)
  @JdbcTypeCode(SqlTypes.JSON)
  private List<AuthorityNote> notes;

  @Column(name = DELETED_COLUMN)
  private boolean deleted = false;

  public AuthorityBase(AuthorityBase other) {
    super(other);
    this.id = other.id;
    this.naturalId = other.naturalId;
    this.authoritySourceFile = Optional.ofNullable(other.authoritySourceFile)
        .map(AuthoritySourceFile::new)
        .orElse(null);
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
    this.deleted = other.deleted;
  }
}
