package org.folio.entlinks.support;

import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.entity.AuthorityDataStat;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

public class DatabaseHelper {
  public static final String AUTHORITY_DATA_STAT = "authority_data_stat";
  public static final String AUTHORITY_DATA = "authority_data";
  private final FolioModuleMetadata metadata;
  private final JdbcTemplate jdbcTemplate;

  public DatabaseHelper(FolioModuleMetadata metadata, JdbcTemplate jdbcTemplate) {
    this.metadata = metadata;
    this.jdbcTemplate = jdbcTemplate;
  }

  public String getTable(String tenantId, String table) {
    return metadata.getDBSchemaName(tenantId) + "." + table;
  }

  public void clearTable(String tenant, String tableName) {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, getTable(tenant, tableName));
  }

  public void saveAuthData(AuthorityData authorityData, String tenant) {
    var sql = "INSERT INTO " + getTable(tenant, AUTHORITY_DATA) + " (id, natural_id, state) VALUES (?, ?, ?)";
    jdbcTemplate.update(sql, authorityData.getId(), authorityData.getNaturalId(), false);
  }

  public void saveStat(AuthorityDataStat stat, String tenant) {
    var sql = "INSERT INTO " + getTable(tenant, AUTHORITY_DATA_STAT) + " (id, authority_id, action,"
      + " authority_natural_id_old, authority_natural_id_new, heading_old, heading_new, heading_type_old,"
      + " heading_type_new, authority_source_file_old, authority_source_file_new, lb_total, lb_updated, lb_failed,"
      + " status, started_by_user_id, started_at, completed_at) VALUES"
      + " (?,?,cast(? as test_mod_entities_links.AuthorityDataStatAction),"
      + "?,?,?,?,?,?,?,?,?,?,?,cast(? as test_mod_entities_links.authoritydatastatstatus),?,?,?)";
    jdbcTemplate.update(sql,
      stat.getId(), stat.getAuthorityData().getId(), stat.getAction().name(), stat.getAuthorityNaturalIdOld(),
      stat.getAuthorityNaturalIdNew(), stat.getHeadingOld(), stat.getHeadingNew(), stat.getHeadingTypeOld(),
      stat.getHeadingTypeNew(), stat.getAuthoritySourceFileOld(), stat.getAuthoritySourceFileNew(),
      stat.getLbTotal(), stat.getLbUpdated(), stat.getLbFailed(), stat.getStatus().name(), stat.getStartedByUserId(),
      stat.getStartedAt(), stat.getCompletedAt());
  }
}

