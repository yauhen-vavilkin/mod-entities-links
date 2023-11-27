package org.folio.entlinks.service.consortium;

import static java.util.Collections.emptyList;
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
import java.util.UUID;
import org.folio.entlinks.domain.entity.InstanceAuthorityLink;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.folio.entlinks.service.consortium.propagation.ConsortiumAuthorityPropagationService;
import org.folio.entlinks.service.consortium.propagation.ConsortiumLinksPropagationService;
import org.folio.entlinks.service.consortium.propagation.model.LinksPropagationData;
import org.folio.entlinks.service.links.InstanceAuthorityLinkingService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.test.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ConsortiumLinksPropagationServiceTest {

  private @Mock InstanceAuthorityLinkingService instanceAuthorityLinkingService;
  private @Mock ConsortiumTenantsService tenantsService;
  private @Mock SystemUserScopedExecutionService executionService;
  private @InjectMocks ConsortiumLinksPropagationService propagationService;

  @Test
  void testPropagateUpdate() {
    var instanceId = UUID.randomUUID();
    var link = new InstanceAuthorityLink();
    link.setInstanceId(instanceId);
    List<InstanceAuthorityLink> links = List.of(link);
    final var propagationData = new LinksPropagationData(instanceId, links);

    doMocks();
    propagationService.propagate(
        propagationData, ConsortiumAuthorityPropagationService.PropagationType.UPDATE, TENANT_ID);

    verify(tenantsService).getConsortiumTenants(TENANT_ID);
    verify(executionService, times(3)).executeAsyncSystemUserScoped(any(), any());
    verify(instanceAuthorityLinkingService, times(3)).updateLinks(instanceId, links);
  }

  @Test
  void testPropagateIllegalPropagationType() {
    final var propagationData = new LinksPropagationData(null, emptyList());

    doMocks();

    var exception = assertThrows(IllegalArgumentException.class,
      () -> propagationService.propagate(propagationData, ConsortiumAuthorityPropagationService.PropagationType.CREATE,
        TENANT_ID));

    assertEquals("Propagation type 'CREATE' is not supported for links.", exception.getMessage());
  }

  @Test
  void testPropagateException() {
    doThrow(FolioIntegrationException.class).when(tenantsService).getConsortiumTenants(any());

    final var propagationData = new LinksPropagationData(null, emptyList());

    propagationService.propagate(
        propagationData, ConsortiumAuthorityPropagationService.PropagationType.UPDATE, TENANT_ID);

    verify(tenantsService, times(1)).getConsortiumTenants(any());
    verify(executionService, times(0)).executeAsyncSystemUserScoped(any(), any());
    verify(instanceAuthorityLinkingService, times(0)).updateLinks(any(), any());
  }

  private void doMocks() {
    when(tenantsService.getConsortiumTenants(TENANT_ID)).thenReturn(List.of("t1", "t2", "t3"));
    doAnswer(invocation -> {
      ((Runnable) invocation.getArgument(1)).run();
      return null;
    }).when(executionService).executeAsyncSystemUserScoped(any(), any());
  }
}

