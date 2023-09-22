package org.folio.entlinks.service.consortium;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.entlinks.client.UserTenantsClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserTenantsService {

  private final UserTenantsClient userTenantsClient;

  public Optional<String> getConsortiumId(String tenantId) {
    if (StringUtils.isBlank(tenantId)) {
      return Optional.empty();
    }

    var userTenantsResponse = userTenantsClient.getUserTenants(tenantId);
    if (userTenantsResponse != null) {
      return userTenantsResponse.userTenants().stream()
        .filter(userTenant -> userTenant.centralTenantId().equals(tenantId))
        .findFirst()
        .map(UserTenantsClient.UserTenant::consortiumId);
    }
    return Optional.empty();
  }
}
