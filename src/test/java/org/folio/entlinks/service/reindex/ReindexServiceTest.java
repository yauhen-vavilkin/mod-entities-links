package org.folio.entlinks.service.reindex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.entity.ReindexJobStatus.IDS_PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.entlinks.domain.entity.ReindexJobResource;
import org.folio.entlinks.domain.entity.ReindexJobStatus;
import org.folio.entlinks.domain.repository.ReindexJobRepository;
import org.folio.entlinks.exception.ReindexJobNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ReindexServiceTest {

  @Mock
  private ReindexJobRepository repository;

  @InjectMocks
  private ReindexService service;

  @Test
  void shouldSubmitReindexJob() {
    var captor = ArgumentCaptor.forClass(ReindexJob.class);
    var expected = new ReindexJob();
    when(repository.save(any(ReindexJob.class))).thenReturn(expected);

    var job = service.submitReindex(ReindexJobResource.AUTHORITY);

    assertThat(job).isEqualTo(expected);
    verify(repository).save(captor.capture());
    var submitted = captor.getValue();
    assertThat(submitted).isNotNull();
    assertThat(submitted.getJobStatus()).isEqualTo(ReindexJobStatus.IN_PROGRESS);
    assertThat(submitted.getResourceName()).isEqualTo(ReindexJobResource.AUTHORITY);
    assertThat(submitted.getPublished()).isZero();
  }

  @Test
  void shouldThrowNoJobFoundExceptionForCancelByGivenId() {
    when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());
    var id = UUID.randomUUID();

    var thrown = assertThrows(ReindexJobNotFoundException.class, () -> service.cancelReindex(id));

    assertThat(thrown.getMessage()).containsOnlyOnce(id.toString());
  }

  @Test
  void shouldThrowIllegalStateExceptionForCancelWhenJobFinishedPublishingEvents() {
    var job = new ReindexJob().withJobStatus(IDS_PUBLISHED);
    when(repository.findById(any(UUID.class))).thenReturn(Optional.of(job));
    var id = UUID.randomUUID();

    var thrown = assertThrows(IllegalStateException.class, () -> service.cancelReindex(id));

    assertThat(thrown.getMessage()).containsOnlyOnce("The job has been finished");
  }

  @Test
  void shouldCancelInProgressReindexJob() {
    var job = new ReindexJob().withJobStatus(ReindexJobStatus.IN_PROGRESS);
    when(repository.findById(any(UUID.class))).thenReturn(Optional.of(job));
    when(repository.save(any(ReindexJob.class))).thenReturn(job);

    var saved = service.cancelReindex(UUID.randomUUID());

    assertThat(saved.getJobStatus()).isEqualTo(ReindexJobStatus.PENDING_CANCEL);
    verify(repository).findById(any(UUID.class));
    verify(repository).save(any(ReindexJob.class));
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotLogProgressOnlyForEvery1000Events() {
    var tracker = new ReindexJobProgressTracker(1500);
    tracker.incrementProcessedCount();
    tracker.incrementProcessedCount();

    service.logJobProgress(tracker, UUID.randomUUID());

    verifyNoInteractions(repository);
  }

  @Test
  void shouldNotLogProgressForCancelledJob() {
    var tracker = mock(ReindexJobProgressTracker.class);
    when(tracker.getProcessedCount()).thenReturn(1000);
    var job = new ReindexJob().withJobStatus(ReindexJobStatus.PENDING_CANCEL);
    when(repository.findById(any(UUID.class))).thenReturn(Optional.of(job));
    var id = UUID.randomUUID();

    var thrown = assertThrows(IllegalStateException.class, () -> service.logJobProgress(tracker, id));

    assertThat(thrown.getMessage()).containsOnlyOnce("The job has been cancelled");
  }

  @Test
  void shouldLogProgressForCancelledJob() {
    var tracker = mock(ReindexJobProgressTracker.class);
    when(tracker.getProcessedCount()).thenReturn(1000);
    var job = new ReindexJob().withJobStatus(ReindexJobStatus.IN_PROGRESS);
    when(repository.findById(any(UUID.class))).thenReturn(Optional.of(job));
    when(repository.saveAndFlush(any(ReindexJob.class))).thenReturn(job);
    var id = UUID.randomUUID();

    service.logJobProgress(tracker, id);

    verify(repository).findById(any(UUID.class));
    verify(repository).saveAndFlush(any(ReindexJob.class));
  }

  @Test
  void shouldLogJobFailed() {
    var job = new ReindexJob().withJobStatus(ReindexJobStatus.IN_PROGRESS);
    when(repository.findById(any(UUID.class))).thenReturn(Optional.of(job));
    when(repository.save(any(ReindexJob.class))).thenReturn(job);
    var id = UUID.randomUUID();
    var captor = ArgumentCaptor.forClass(ReindexJob.class);

    service.logJobFailed(id);

    verify(repository).save(captor.capture());
    var logged = captor.getValue();
    assertThat(logged.getJobStatus()).isEqualTo(ReindexJobStatus.ID_PUBLISHING_FAILED);
  }

  @Test
  void shouldThrowJobNotFoundWhenLoggingJobFailed() {
    when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());
    var id = UUID.randomUUID();

    var thrown = assertThrows(ReindexJobNotFoundException.class, () -> service.logJobFailed(id));

    assertThat(thrown.getMessage()).containsOnlyOnce(id.toString());
  }

  @Test
  void shouldGetAllReindexJobs() {
    var page = new PageImpl<ReindexJob>(List.of());
    when(repository.findAll(any(Pageable.class))).thenReturn(page);

    var result = service.getAllReindexJobs("", 0, 10);

    assertThat(result).isEqualTo(page);
    verify(repository).findAll(any(Pageable.class));
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldGetAllReindexJobsByQuery() {
    var page = new PageImpl<ReindexJob>(List.of());
    when(repository.findByCql(any(String.class), any(Pageable.class))).thenReturn(page);

    var result = service.getAllReindexJobs("query", 0, 10);

    assertThat(result).isEqualTo(page);
    verify(repository).findByCql(any(String.class), any(Pageable.class));
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldLogJobSuccess() {
    var expected = new ReindexJob();
    when(repository.findById(any(UUID.class))).thenReturn(Optional.of(expected));

    service.logJobSuccess(UUID.randomUUID());

    assertEquals(IDS_PUBLISHED, expected.getJobStatus());
  }
}
