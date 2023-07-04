package org.folio.entlinks.controller.delegate;

import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.converter.ReindexJobMapper;
import org.folio.entlinks.domain.dto.ReindexJobDto;
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

}
