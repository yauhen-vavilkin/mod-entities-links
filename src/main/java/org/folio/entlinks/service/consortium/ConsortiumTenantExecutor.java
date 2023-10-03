package org.folio.entlinks.service.consortium;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class ConsortiumTenantExecutor {

  private final UserTenantsService userTenantsService;
  private final FolioExecutionContext folioExecutionContext;
  private final SystemUserScopedExecutionService scopedExecutionService;

  public <T> T executeAsCentralTenant(Supplier<T> operation) {
    var tenantId = folioExecutionContext.getTenantId();
    var centralTenantId = userTenantsService.getCentralTenant(tenantId);

    if (centralTenantId.isEmpty()) {
      log.warn("Tenant: {} is not in consortia", tenantId);
      return null;
    }

    log.info("Changing context from {} to {}", tenantId, centralTenantId);
    return scopedExecutionService.executeSystemUserScoped(centralTenantId.get(), operation::get);
  }

}
