package org.folio.entlinks.service.tenant;

import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.service.dataloader.ReferenceDataLoader;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.service.TenantService;
import org.folio.spring.tools.kafka.KafkaAdminService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Primary
@Service
@Log4j2
public class ExtendedTenantService extends TenantService {

  private final PrepareSystemUserService folioPrepareSystemUserService;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;
  private final FolioExecutionContext folioExecutionContext;
  private final KafkaAdminService kafkaAdminService;
  private final ReferenceDataLoader referenceDataLoader;

  public ExtendedTenantService(JdbcTemplate jdbcTemplate,
                               FolioExecutionContext context,
                               KafkaAdminService kafkaAdminService,
                               FolioSpringLiquibase folioSpringLiquibase,
                               FolioExecutionContext folioExecutionContext,
                               PrepareSystemUserService folioPrepareSystemUserService,
                               SystemUserScopedExecutionService systemUserScopedExecutionService,
                               ReferenceDataLoader referenceDataLoader) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.folioPrepareSystemUserService = folioPrepareSystemUserService;
    this.folioExecutionContext = folioExecutionContext;
    this.kafkaAdminService = kafkaAdminService;
    this.systemUserScopedExecutionService = systemUserScopedExecutionService;
    this.referenceDataLoader = referenceDataLoader;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    super.afterTenantUpdate(tenantAttributes);
    kafkaAdminService.createTopics(folioExecutionContext.getTenantId());
    kafkaAdminService.restartEventListeners();
    folioPrepareSystemUserService.setupSystemUser();
  }

  @Override
  protected void afterTenantDeletion(TenantAttributes tenantAttributes) {
    var tenantId = context.getTenantId();
    kafkaAdminService.deleteTopics(tenantId);
  }

  @Override
  public void loadReferenceData() {
    systemUserScopedExecutionService.executeSystemUserScoped(context.getTenantId(),
      () -> {
        referenceDataLoader.loadRefData();
        return null;
      });
  }
}
