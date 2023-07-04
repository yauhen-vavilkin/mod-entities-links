package org.folio.entlinks.service.reindex;

import static org.folio.entlinks.domain.entity.ReindexJobStatus.IDS_PUBLISHED;
import static org.folio.entlinks.domain.entity.ReindexJobStatus.ID_PUBLISHING_CANCELLED;
import static org.folio.entlinks.domain.entity.ReindexJobStatus.ID_PUBLISHING_FAILED;
import static org.folio.entlinks.domain.entity.ReindexJobStatus.IN_PROGRESS;
import static org.folio.entlinks.domain.entity.ReindexJobStatus.PENDING_CANCEL;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.entlinks.domain.entity.ReindexJobResource;
import org.folio.entlinks.domain.repository.ReindexJobRepository;
import org.folio.entlinks.exception.ReindexJobNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReindexService {

  private final ReindexJobRepository reindexJobRepository;

  @Transactional
  public ReindexJob submitReindex(ReindexJobResource reindexResourceName) {
    var reindexJob = reindexJobRepository.save(buildInitialJob(reindexResourceName));
    return reindexJob;
  }

  @Transactional
  public ReindexJob cancelReindex(UUID jobId) {
    var jobOptional = reindexJobRepository.findById(jobId);
    if (jobOptional.isPresent()) {
      var reindexJob = jobOptional.get();
      if (reindexJob.getJobStatus() == IDS_PUBLISHED) {
        throw new IllegalStateException("The job has been finished");
      }
      reindexJob.setJobStatus(PENDING_CANCEL);
      return reindexJob;
    } else {
      throw new ReindexJobNotFoundException(jobId);
    }
  }

  @Transactional
  public void logJobProgress(ReindexJobProgressTracker progressTracker, ReindexContext context) {
    if (!shouldLogJobProgress(progressTracker)) {
      return;
    }
    var jobOptional = reindexJobRepository.findById(context.getJobId());
    if (jobOptional.isPresent()) {
      var reindexJob = jobOptional.get();
      if (reindexJob.getJobStatus() == PENDING_CANCEL) {
        throw new IllegalStateException("The job has been cancelled");
      }
      reindexJob.setPublished(progressTracker.getProcessedCount());
      reindexJobRepository.saveAndFlush(reindexJob);
    }
  }

  @Transactional
  public void logJobFailed(ReindexContext context) {
    reindexJobRepository.findById(context.getJobId())
      .map(reindexJob -> {
        var finalStatus = reindexJob.getJobStatus() == PENDING_CANCEL
                          ? ID_PUBLISHING_CANCELLED
                          : ID_PUBLISHING_FAILED;
        return reindexJob.withJobStatus(finalStatus);
      });
  }

  @Transactional
  public void logJobSuccess(ReindexContext context) {
    reindexJobRepository.findById(context.getJobId())
      .map(reindexJob -> reindexJob.withJobStatus(IDS_PUBLISHED));
  }

  private boolean shouldLogJobProgress(ReindexJobProgressTracker progressTracker) {
    return progressTracker.getProcessedCount() % 1000 == 0;
  }

  private ReindexJob buildInitialJob(ReindexJobResource reindexResourceName) {
    return new ReindexJob()
      .withJobStatus(IN_PROGRESS)
      .withResourceName(reindexResourceName)
      .withPublished(0);
  }
}
