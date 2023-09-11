package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class MetadataEntity {

  public static final String CREATED_DATE_COLUMN = "created_date";
  public static final String UPDATED_DATE_COLUMN = "updated_date";
  public static final String CREATED_BY_USER_COLUMN = "created_by_user_id";
  public static final String UPDATED_BY_USER_COLUMN = "updated_by_user_id";

  @CreatedDate
  @Column(name = "created_date", nullable = false, updatable = false)
  private Timestamp createdDate;

  @LastModifiedDate
  @Column(name = "updated_date", nullable = false)
  private Timestamp updatedDate;

  @CreatedBy
  @Column(name = "created_by_user_id", nullable = false, updatable = false)
  private UUID createdByUserId;

  @LastModifiedBy
  @Column(name = "updated_by_user_id", nullable = false)
  private UUID updatedByUserId;

  public MetadataEntity(MetadataEntity other) {
    this.createdDate = other.createdDate;
    this.createdByUserId = other.createdByUserId;
    this.updatedDate = other.updatedDate;
    this.updatedByUserId = other.updatedByUserId;
  }
}
