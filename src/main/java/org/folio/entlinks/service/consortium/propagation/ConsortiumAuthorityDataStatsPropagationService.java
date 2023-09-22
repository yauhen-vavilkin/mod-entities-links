package org.folio.entlinks.service.consortium.propagation;

import java.util.List;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

@Service
public class ConsortiumAuthorityDataStatsPropagationService
  extends ConsortiumPropagationService<List<AuthorityDataStat>> {

  private static final String ILLEGAL_PROPAGATION_MSG = "Propagation type '%s' is not supported for data stats.";

  private final AuthorityDataStatService authorityDataStatService;

  protected ConsortiumAuthorityDataStatsPropagationService(ConsortiumTenantsService tenantsService,
                                                           SystemUserScopedExecutionService executionService,
                                                           AuthorityDataStatService authorityDataStatService) {
    super(tenantsService, executionService);
    this.authorityDataStatService = authorityDataStatService;
  }

  @Override
  protected void doPropagation(List<AuthorityDataStat> authorityDataStats,
                               ConsortiumAuthorityPropagationService.PropagationType propagationType) {
    switch (propagationType) {
      case CREATE -> authorityDataStatService.createInBatch(authorityDataStats);
      case UPDATE, DELETE -> throw new IllegalArgumentException(ILLEGAL_PROPAGATION_MSG.formatted(propagationType));
      default -> throw new IllegalStateException("Unexpected value: " + propagationType);
    }
  }
}
