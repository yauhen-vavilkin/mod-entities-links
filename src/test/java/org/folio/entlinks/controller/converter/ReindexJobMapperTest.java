package org.folio.entlinks.controller.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.entlinks.domain.entity.ReindexJobResource.AUTHORITY;
import static org.folio.entlinks.domain.entity.ReindexJobStatus.IN_PROGRESS;
import static org.folio.entlinks.utils.DateUtils.fromTimestamp;
import static org.folio.support.base.TestConstants.TEST_DATE;
import static org.folio.support.base.TestConstants.TEST_ID;

import java.util.List;
import org.folio.entlinks.domain.dto.ReindexJobDto;
import org.folio.entlinks.domain.dto.ReindexJobDtoCollection;
import org.folio.entlinks.domain.entity.ReindexJob;
import org.folio.spring.test.type.UnitTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@UnitTest
class ReindexJobMapperTest {

  private final ReindexJobMapper mapper = new ReindexJobMapperImpl();

  @Test
  void testToDto() {
    ReindexJob reindexJob = createReindexJob();

    ReindexJobDto dto = mapper.toDto(reindexJob);

    assertThat(dto.getId()).isEqualTo(reindexJob.getId());
    assertThat(dto.getPublished()).isEqualTo(reindexJob.getPublished());
    assertThat(dto.getJobStatus().name()).isEqualTo(reindexJob.getJobStatus().name());
    assertThat(dto.getResourceName().name()).isEqualTo(reindexJob.getResourceName().name());

  }

  @Test
  void testToDtoList() {
    ReindexJob job = createReindexJob();
    List<ReindexJob> jobList = List.of(job);

    List<ReindexJobDto> dtoList = mapper.toDtoList(jobList);

    assertThat(dtoList).hasSize(1);
    ReindexJobDto dto = dtoList.get(0);
    assertThat(dto.getId()).isEqualTo(job.getId());
    assertThat(dto.getPublished()).isEqualTo(job.getPublished());
    assertThat(dto.getJobStatus().name()).isEqualTo(job.getJobStatus().name());
    assertThat(dto.getResourceName().name()).isEqualTo(job.getResourceName().name());
  }

  @Test
  void testToReindexJobCollection() {
    ReindexJob job = createReindexJob();

    List<ReindexJob> jobList = List.of(job);
    Page<ReindexJob> page = new PageImpl<>(jobList);

    ReindexJobDtoCollection dtoCollection = mapper.toReindexJobCollection(page);

    assertThat(dtoCollection.getTotalRecords()).isEqualTo(1);
    assertThat(dtoCollection.getReindexJobs()).hasSize(1);
    ReindexJobDto dto = dtoCollection.getReindexJobs().get(0);
    assertThat(dto.getId()).isEqualTo(job.getId());
    assertThat(dto.getPublished()).isEqualTo(job.getPublished());
    assertThat(dto.getJobStatus().name()).isEqualTo(job.getJobStatus().name());
    assertThat(dto.getResourceName().name()).isEqualTo(job.getResourceName().name());
  }

  @NotNull
  private static ReindexJob createReindexJob() {
    ReindexJob reindexJob = new ReindexJob();
    reindexJob.setId(TEST_ID);
    reindexJob.setPublished(1);
    reindexJob.setJobStatus(IN_PROGRESS);
    reindexJob.setResourceName(AUTHORITY);
    reindexJob.setSubmittedDate(fromTimestamp(TEST_DATE));
    return reindexJob;
  }

}
