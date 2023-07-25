package org.folio.entlinks.controller.delegate;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.converter.ReindexJobMapper;
import org.folio.entlinks.domain.dto.ReindexJobDto;
import org.folio.entlinks.domain.dto.ReindexJobDtoCollection;
import org.folio.entlinks.domain.entity.ReindexJobResource;
import org.folio.entlinks.service.reindex.AuthorityReindexJobRunner;
import org.folio.entlinks.service.reindex.ReindexService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReindexServiceDelegate {

  private final ReindexService service;
  private final ApplicationContext applicationContext;
  private final ReindexJobMapper jobMapper;

  public ReindexJobDto startAuthoritiesReindex() {
    var reindexJob = service.submitReindex(ReindexJobResource.AUTHORITY);
    var jobRunner = applicationContext.getBean(AuthorityReindexJobRunner.class);
    jobRunner.startReindex(reindexJob);
    return jobMapper.toDto(reindexJob);
  }

  public ReindexJobDtoCollection retrieveReindexJobs(String query, Integer offset, Integer limit) {
    var jobs = service.getAllReindexJobs(query, offset, limit);
    return jobMapper.toReindexJobCollection(jobs);
  }

  public ReindexJobDto getReindexJobById(UUID id) {
    var job = service.getById(id);
    return jobMapper.toDto(job);
  }

  public void deleteReindexJob(UUID id) {
    service.cancelReindex(id);
  }
}
