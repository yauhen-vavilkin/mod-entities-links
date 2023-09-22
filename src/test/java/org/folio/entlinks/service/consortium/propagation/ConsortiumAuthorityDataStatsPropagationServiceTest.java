package org.folio.entlinks.service.consortium.propagation;

import static org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityPropagationService.PropagationType.CREATE;
import static org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityPropagationService.PropagationType.UPDATE;
import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.service.consortium.ConsortiumTenantsService;
import org.folio.entlinks.service.links.AuthorityDataStatService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ConsortiumAuthorityDataStatsPropagationServiceTest {

  private @Mock AuthorityDataStatService authorityDataStatService;
  private @Mock ConsortiumTenantsService tenantsService;
  private @Mock SystemUserScopedExecutionService executionService;
  private @InjectMocks ConsortiumAuthorityDataStatsPropagationService propagationService;

  @Test
  void testPropagateCreate() {
    List<AuthorityDataStat> stats = List.of(new AuthorityDataStat());

    doMocks();
    propagationService.propagate(stats, CREATE, TENANT_ID);

    verify(tenantsService).getConsortiumTenants(TENANT_ID);
    verify(executionService, times(3)).executeAsyncSystemUserScoped(any(), any());
    verify(authorityDataStatService, times(3)).createInBatch(stats);
  }

  @Test
  void testPropagateIllegalPropagationType() {
    List<AuthorityDataStat> stats = List.of(new AuthorityDataStat());

    doMocks();

    var exception = assertThrows(IllegalArgumentException.class,
      () -> propagationService.propagate(stats, UPDATE, TENANT_ID));

    assertEquals("Propagation type 'UPDATE' is not supported for data stats.", exception.getMessage());
  }

  @Test
  void testPropagateException() {
    doThrow(FolioIntegrationException.class).when(tenantsService).getConsortiumTenants(any());

    List<AuthorityDataStat> stats = List.of(new AuthorityDataStat());

    propagationService.propagate(stats, CREATE, TENANT_ID);

    verify(tenantsService, times(1)).getConsortiumTenants(any());
    verify(executionService, times(0)).executeAsyncSystemUserScoped(any(), any());
    verify(authorityDataStatService, times(0)).createInBatch(any());
  }

  private void doMocks() {
    when(tenantsService.getConsortiumTenants(TENANT_ID)).thenReturn(List.of("t1", "t2", "t3"));
    doAnswer(invocation -> {
      ((Runnable) invocation.getArgument(1)).run();
      return null;
    }).when(executionService).executeAsyncSystemUserScoped(any(), any());
  }
}
