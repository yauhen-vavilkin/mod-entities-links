package org.folio.entlinks.service.consortium;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.folio.entlinks.client.UserTenantsClient;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UserTenantsServiceTest {

  @Mock
  private UserTenantsClient userTenantsClient;

  @InjectMocks
  private UserTenantsService userTenantsService;

  @Test
  void testGetConsortiumIdWithBlankTenantId() {
    String tenantId = "";

    Optional<String> consortiumId = userTenantsService.getConsortiumId(tenantId);

    assertTrue(consortiumId.isEmpty());
    verifyNoInteractions(userTenantsClient); // Ensure that userTenantsClient is not called.
  }

  @Test
  void testGetConsortiumIdWithNullResponse() {
    String tenantId = "tenant1";

    // Mock the userTenantsClient to return null response
    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(null);

    Optional<String> consortiumId = userTenantsService.getConsortiumId(tenantId);

    assertTrue(consortiumId.isEmpty());
    verify(userTenantsClient).getUserTenants(tenantId);
  }

  @Test
  void testGetConsortiumIdWithNoMatchingUserTenants() {
    String tenantId = "tenant1";

    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(new UserTenantsClient.UserTenants(emptyList()));

    Optional<String> consortiumId = userTenantsService.getConsortiumId(tenantId);

    assertTrue(consortiumId.isEmpty());
    verify(userTenantsClient).getUserTenants(tenantId);
  }

  @Test
  void testGetConsortiumIdWithMatchingUserTenant() {
    String tenantId = "tenant1";
    String consortiumIdValue = "consortium123";

    // Mock the userTenantsClient to return a matching UserTenant
    UserTenantsClient.UserTenant userTenant = new UserTenantsClient.UserTenant(tenantId, consortiumIdValue);

    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(
      new UserTenantsClient.UserTenants(singletonList(userTenant)));

    Optional<String> consortiumId = userTenantsService.getConsortiumId(tenantId);

    assertTrue(consortiumId.isPresent());
    assertEquals(consortiumIdValue, consortiumId.get());
    verify(userTenantsClient).getUserTenants(tenantId);
  }
}
