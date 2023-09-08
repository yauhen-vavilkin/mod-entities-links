package org.folio.entlinks.controller.delegate;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.converter.ReindexJobMapper;
import org.folio.entlinks.domain.dto.ReindexJobDto;
import org.folio.entlinks.domain.dto.ReindexJobDtoCollection;
import org.folio.entlinks.domain.entity.ReindexJobResource;
import org.folio.entlinks.service.reindex.AuthorityReindexJobRunner;
import org.folio.entlinks.service.reindex.ReindexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReindexServiceDelegate {

  private final ReindexService service;
  private final ReindexJobMapper jobMapper;

  @Autowired
  private AuthorityReindexJobRunner jobRunner;

  public ReindexJobDto startAuthoritiesReindex() {
    var reindexJob = service.submitReindex(ReindexJobResource.AUTHORITY);
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
