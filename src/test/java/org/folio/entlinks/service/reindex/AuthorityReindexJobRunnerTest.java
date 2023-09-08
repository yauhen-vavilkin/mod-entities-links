package org.folio.entlinks.service.reindex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.stream.Stream;
import org.folio.entlinks.domain.dto.AuthorityDto;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.entlinks.domain.entity.ReindexJobResource;
import org.folio.entlinks.service.authority.AuthorityDomainEventPublisher;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AuthorityReindexJobRunnerTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private AuthorityDomainEventPublisher eventPublisher;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Mock
  private ReindexService reindexService;

  @InjectMocks
  private AuthorityReindexJobRunner jobRunner;

  @Test
  void shouldInitiateReindexJob() {
    var id = UUID.randomUUID();
    var expectedDto = new AuthorityDto();
    expectedDto.setId(id);
    expectedDto.setPersonalName("personal_name");
    expectedDto.setSource("source");
    expectedDto.setVersion(1);
    expectedDto.setSourceFileId(id);
    expectedDto.setNaturalId("10");
    expectedDto.setSubjectHeadings("a");

    when(jdbcTemplate.queryForObject(any(), eq(Integer.class))).thenReturn(1);
    when(jdbcTemplate.queryForStream(anyString(), any())).thenReturn(Stream.of(expectedDto));
    var authorityCaptor = ArgumentCaptor.forClass(AuthorityDto.class);
    var progressCaptor = ArgumentCaptor.forClass(ReindexJobProgressTracker.class);
    var jobIdCaptor = ArgumentCaptor.forClass(UUID.class);

    var reindexJob = new ReindexJob().withResourceName(ReindexJobResource.AUTHORITY).withId(UUID.randomUUID());

    jobRunner.startReindex(reindexJob);

    verify(eventPublisher).publishReindexEvent(authorityCaptor.capture(), any(ReindexContext.class));
    verify(reindexService).logJobProgress(progressCaptor.capture(), jobIdCaptor.capture());
    var dto = authorityCaptor.getValue();
    assertEquals(expectedDto, dto);
    assertEquals(reindexJob.getId(), jobIdCaptor.getValue());
    var progressTracker = progressCaptor.getValue();
    assertEquals(1, progressTracker.getProcessedCount());
    assertEquals(1, progressTracker.getTotalRecords());
    verify(reindexService).logJobSuccess(jobIdCaptor.capture());
    assertEquals(reindexJob.getId(), jobIdCaptor.getValue());
  }
}
