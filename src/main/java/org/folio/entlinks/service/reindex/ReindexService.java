package org.folio.entlinks.service.reindex;

import static org.folio.entlinks.domain.entity.ReindexJobStatus.IDS_PUBLISHED;
import static org.folio.entlinks.domain.entity.ReindexJobStatus.ID_PUBLISHING_CANCELLED;
import static org.folio.entlinks.domain.entity.ReindexJobStatus.ID_PUBLISHING_FAILED;
import static org.folio.entlinks.domain.entity.ReindexJobStatus.IN_PROGRESS;
import static org.folio.entlinks.domain.entity.ReindexJobStatus.PENDING_CANCEL;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.entlinks.domain.entity.ReindexJobResource;
import org.folio.entlinks.domain.repository.ReindexJobRepository;
import org.folio.entlinks.exception.ReindexJobNotFoundException;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReindexService {

  private final ReindexJobRepository repository;

  @Transactional
  public ReindexJob submitReindex(ReindexJobResource reindexResourceName) {
    return repository.save(buildInitialJob(reindexResourceName));
  }

  public Page<ReindexJob> getAllReindexJobs(String query, Integer offset, Integer limit) {
    log.debug("getAllReindexJobs:: Attempts to find all ReindexJobs by [offset: {}, limit: {}, cql: {}]",
        offset, limit, query);

    if (StringUtils.isBlank(query)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(query, new OffsetRequest(offset, limit));
  }

  public ReindexJob getById(UUID id) {
    return repository.findById(id).orElseThrow(() -> new ReindexJobNotFoundException(id));
  }

  @Transactional
  public ReindexJob cancelReindex(UUID jobId) {
    var jobOptional = repository.findById(jobId);
    if (jobOptional.isPresent()) {
      var reindexJob = jobOptional.get();
      if (reindexJob.getJobStatus() == IDS_PUBLISHED) {
        throw new IllegalStateException("The job has been finished");
      }
      reindexJob.setJobStatus(PENDING_CANCEL);
      return repository.save(reindexJob);
    } else {
      throw new ReindexJobNotFoundException(jobId);
    }
  }

  @Transactional
  public void logJobProgress(ReindexJobProgressTracker progressTracker, UUID jobId) {
    if (!shouldLogJobProgress(progressTracker)) {
      return;
    }
    var jobOptional = repository.findById(jobId);
    if (jobOptional.isPresent()) {
      var reindexJob = jobOptional.get();
      if (reindexJob.getJobStatus() == PENDING_CANCEL) {
        throw new IllegalStateException("The job has been cancelled");
      }
      reindexJob.setPublished(progressTracker.getProcessedCount());
      repository.saveAndFlush(reindexJob);
    }
  }

  @Transactional
  public void logJobFailed(UUID jobId) {
    var failedJob = repository.findById(jobId)
      .map(reindexJob -> {
        var finalStatus = reindexJob.getJobStatus() == PENDING_CANCEL
                          ? ID_PUBLISHING_CANCELLED
                          : ID_PUBLISHING_FAILED;
        return reindexJob.withJobStatus(finalStatus);
      })
        .map(repository::save)
        .orElseThrow(() -> new ReindexJobNotFoundException(jobId));

    log.debug("Job: {} has been failed", failedJob);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logJobSuccess(UUID jobId) {
    var existingJob = repository.findById(jobId).orElseThrow(() -> new ReindexJobNotFoundException(jobId));
    existingJob.setJobStatus(IDS_PUBLISHED);

    repository.save(existingJob);
  }

  private boolean shouldLogJobProgress(ReindexJobProgressTracker progressTracker) {
    return progressTracker.getProcessedCount() % 1000 == 0;
  }

  private ReindexJob buildInitialJob(ReindexJobResource reindexResourceName) {
    return new ReindexJob()
        .withJobStatus(IN_PROGRESS)
        .withResourceName(reindexResourceName)
        .withPublished(0)
        .withSubmittedDate(OffsetDateTime.now());
  }
}
