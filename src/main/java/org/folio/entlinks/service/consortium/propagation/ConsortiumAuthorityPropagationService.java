package org.folio.entlinks.service.consortium.propagation;

import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class ConsortiumAuthorityPropagationService extends ConsortiumPropagationService<Authority> {

  private final AuthorityService authorityService;

  public ConsortiumAuthorityPropagationService(AuthorityService authorityService,
                                               ConsortiumTenantsService tenantsService,
                                               SystemUserScopedExecutionService executionService) {
    super(tenantsService, executionService);
    this.authorityService = authorityService;
  }

  protected void doPropagation(Authority authority, PropagationType propagationType) {
    authority.makeAsConsortiumShadowCopy();
    switch (propagationType) {
      case CREATE -> authorityService.create(authority);
      case UPDATE -> authorityService.update(authority.getId(), authority);
      case DELETE -> authorityService.deleteById(authority.getId());
      default -> throw new IllegalStateException("Unexpected value: " + propagationType);
    }
  }

  public enum PropagationType {
    CREATE, UPDATE, DELETE
  }
}
