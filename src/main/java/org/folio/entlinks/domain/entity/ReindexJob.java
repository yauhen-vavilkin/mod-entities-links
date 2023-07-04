package org.folio.entlinks.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

@Getter
@Setter
@With
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "reindex_job")
public class ReindexJob {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_name", nullable = false)
  private ReindexJobResource resourceName;

  @Enumerated(EnumType.STRING)
  @Column(name = "job_status", nullable = false)
  private ReindexJobStatus jobStatus;

  @Column(name = "published")
  private Integer published;

  @Column(name = "submitted_date")
  private OffsetDateTime submittedDate;

}
