package org.folio.entlinks.service;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.spring.tools.kafka.KafkaAdminService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Primary
@Service
public class LinksTenantService extends TenantService {

  private final FolioExecutionContext folioExecutionContext;
  private final KafkaAdminService kafkaAdminService;

  public LinksTenantService(JdbcTemplate jdbcTemplate,
                            FolioExecutionContext context,
                            FolioSpringLiquibase folioSpringLiquibase,
                            KafkaAdminService kafkaAdminService) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.folioExecutionContext = context;
    this.kafkaAdminService = kafkaAdminService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    super.afterTenantUpdate(tenantAttributes);
    kafkaAdminService.createTopics(folioExecutionContext.getTenantId());
    kafkaAdminService.restartEventListeners();
  }

}
