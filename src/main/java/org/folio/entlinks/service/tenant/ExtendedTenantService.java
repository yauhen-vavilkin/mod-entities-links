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

  private static final String RECORDS_LB_VIEW_SQL = """
    CREATE OR REPLACE VIEW %s.records_lb_view
          AS
        SELECT *
          FROM %s_mod_source_record_storage.records_lb;
    """;
  private static final String MARC_RECORDS_LB_VIEW_SQL = """
    CREATE OR REPLACE VIEW %s.marc_records_lb_view
          AS
        SELECT *
          FROM %s_mod_source_record_storage.marc_records_lb;
    """;
  private static final String AUTHORITY_VIEW_SQL = """
    CREATE OR REPLACE VIEW %s.authority_view
          AS
        SELECT *
          FROM %s_mod_inventory_storage.authority;
    """;
  private static final String MARC_AUTHORITY_VIEW_SQL = """
    CREATE OR REPLACE VIEW %1$s.marc_authority_view
           AS
         SELECT r.id as marc_id, r.external_id as authority_id,
         mr.content as marc, r.state, (a.jsonb->'_version')::integer as v
         FROM %1$s.records_lb_view r
         LEFT JOIN %1$s.marc_records_lb_view mr ON mr.id = r.id
         LEFT JOIN %1$s.authority_view a ON a.id = r.external_id
         WHERE r.state = 'ACTUAL' AND r.record_type = 'MARC_AUTHORITY'
         ORDER BY r.id;
     """;

  private final PrepareSystemUserService prepareSystemUserService;
  private final KafkaAdminService kafkaAdminService;

  public ExtendedTenantService(JdbcTemplate jdbcTemplate,
                               FolioExecutionContext context,
                               KafkaAdminService kafkaAdminService,
                               FolioSpringLiquibase folioSpringLiquibase,
                               PrepareSystemUserService prepareSystemUserService) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.prepareSystemUserService = prepareSystemUserService;
    this.kafkaAdminService = kafkaAdminService;
  }

  @Override
  protected void afterLiquibaseUpdate(TenantAttributes tenantAttributes) {
    super.afterLiquibaseUpdate(tenantAttributes);
    var tenantId = context.getTenantId();
    var schemaName = getSchemaName();
    jdbcTemplate.execute(String.format(RECORDS_LB_VIEW_SQL, schemaName, tenantId));
    jdbcTemplate.execute(String.format(MARC_RECORDS_LB_VIEW_SQL, schemaName, tenantId));
    jdbcTemplate.execute(String.format(AUTHORITY_VIEW_SQL, schemaName, tenantId));
    jdbcTemplate.execute(String.format(MARC_AUTHORITY_VIEW_SQL, schemaName));
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    super.afterTenantUpdate(tenantAttributes);
    kafkaAdminService.createTopics(context.getTenantId());
    kafkaAdminService.restartEventListeners();
    prepareSystemUserService.setupSystemUser();
  }
}
