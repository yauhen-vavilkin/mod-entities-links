package org.folio.entlinks.service;

import static org.folio.support.base.TestConstants.TENANT_ID;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.tools.kafka.KafkaAdminService;
import org.folio.support.types.UnitTest;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LinksTenantServiceTest {

  @InjectMocks
  private LinksTenantService tenantService;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private KafkaAdminService kafkaAdminService;

  @Test
  void initializeTenant_positive() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
    doNothing().when(kafkaAdminService).createTopics(TENANT_ID);
    doNothing().when(kafkaAdminService).restartEventListeners();

    tenantService.afterTenantUpdate(tenantAttributes());

    verify(kafkaAdminService).createTopics(TENANT_ID);
    verify(kafkaAdminService).restartEventListeners();
  }

  private TenantAttributes tenantAttributes() {
    return new TenantAttributes().moduleTo("mod-search");
  }
}
