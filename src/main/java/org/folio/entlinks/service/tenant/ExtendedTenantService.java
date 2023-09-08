package org.folio.entlinks.service.tenant;

import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.spring.tools.kafka.KafkaAdminService;
import org.folio.spring.tools.systemuser.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Primary
@Service
@Log4j2
public class ExtendedTenantService extends TenantService {

  private final PrepareSystemUserService folioPrepareSystemUserService;
  private final FolioExecutionContext folioExecutionContext;
  private final KafkaAdminService kafkaAdminService;

  public ExtendedTenantService(JdbcTemplate jdbcTemplate,
                               FolioExecutionContext context,
                               KafkaAdminService kafkaAdminService,
                               FolioSpringLiquibase folioSpringLiquibase,
                               FolioExecutionContext folioExecutionContext,
                               PrepareSystemUserService folioPrepareSystemUserService) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.folioPrepareSystemUserService = folioPrepareSystemUserService;
    this.folioExecutionContext = folioExecutionContext;
    this.kafkaAdminService = kafkaAdminService;
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
}
