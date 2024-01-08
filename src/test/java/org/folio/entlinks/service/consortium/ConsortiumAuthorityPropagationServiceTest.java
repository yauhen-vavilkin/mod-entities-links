package org.folio.entlinks.service.consortium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.support.base.TestConstants.AUTHORITY_CONSORTIUM_SOURCE;
import static org.folio.support.base.TestConstants.AUTHORITY_SOURCE;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.service.authority.AuthorityService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityPropagationService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ConsortiumAuthorityPropagationServiceTest {

  private @Mock AuthorityService authorityService;
  private @Mock ConsortiumTenantsService tenantsService;
  private @Mock SystemUserScopedExecutionService executionService;
  private @InjectMocks ConsortiumAuthorityPropagationService propagationService;

  @Test
  void testPropagateCreate() throws FolioIntegrationException {
    var authority = authority();

    doMocks();
    propagationService.propagate(authority, ConsortiumAuthorityPropagationService.PropagationType.CREATE, TENANT_ID);

    assertThat(authority.getSource()).isEqualTo(AUTHORITY_CONSORTIUM_SOURCE);
    verify(tenantsService).getConsortiumTenants(TENANT_ID);
    verify(executionService, times(3)).executeAsyncSystemUserScoped(any(), any());
    verify(authorityService, times(3)).create(authority);
  }

  @Test
  void testPropagateUpdate() throws FolioIntegrationException {
    var authority = authority();
    authority.setId(UUID.randomUUID());
    doMocks();
    propagationService.propagate(authority, ConsortiumAuthorityPropagationService.PropagationType.UPDATE, TENANT_ID);

    assertThat(authority.getSource()).isEqualTo(AUTHORITY_CONSORTIUM_SOURCE);
    verify(tenantsService, times(1)).getConsortiumTenants(any());
    verify(executionService, times(3)).executeAsyncSystemUserScoped(any(), any());
    verify(authorityService, times(3)).update(authority.getId(), authority);
  }

  @Test
  void testPropagateDelete() throws FolioIntegrationException {
    var authority = authority();
    authority.setId(UUID.randomUUID());
    doMocks();
    propagationService.propagate(authority, ConsortiumAuthorityPropagationService.PropagationType.DELETE, TENANT_ID);

    assertThat(authority.getSource()).isEqualTo(AUTHORITY_CONSORTIUM_SOURCE);
    verify(tenantsService, times(1)).getConsortiumTenants(any());
    verify(executionService, times(3)).executeAsyncSystemUserScoped(any(), any());
    verify(authorityService, times(3)).deleteById(authority.getId());
  }

  @Test
  void testPropagateException() throws FolioIntegrationException {
    Mockito.doThrow(FolioIntegrationException.class).when(tenantsService).getConsortiumTenants(any());

    var authority = authority();
    propagationService.propagate(authority, ConsortiumAuthorityPropagationService.PropagationType.CREATE, TENANT_ID);

    verify(tenantsService, times(1)).getConsortiumTenants(any());
    verify(executionService, times(0)).executeAsyncSystemUserScoped(any(), any());
    verify(authorityService, times(0)).create(any());
  }

  private Authority authority() {
    var authority = new Authority();
    authority.setSource(AUTHORITY_SOURCE);
    return authority;
  }

  private void doMocks() {
    when(tenantsService.getConsortiumTenants(TENANT_ID)).thenReturn(List.of("t1", "t2", "t3"));
    doAnswer(invocation -> {
      ((Runnable) invocation.getArgument(1)).run();
      return null;
    }).when(executionService).executeAsyncSystemUserScoped(any(), any());
  }

}
