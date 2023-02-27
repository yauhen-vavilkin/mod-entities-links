package org.folio.support;

import org.folio.spring.FolioModuleMetadata;
import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseHelper {

  public static final String AUTHORITY_DATA_STAT_TABLE = "authority_data_stat";
  public static final String INSTANCE_AUTHORITY_LINK_TABLE = "instance_authority_link";
  public static final String AUTHORITY_DATA_TABLE = "authority_data";

  private final FolioModuleMetadata metadata;
  private final JdbcTemplate jdbcTemplate;

  public DatabaseHelper(FolioModuleMetadata metadata, JdbcTemplate jdbcTemplate) {
    this.metadata = metadata;
    this.jdbcTemplate = jdbcTemplate;
  }

  public String getTable(String tenantId, String table) {
    return metadata.getDBSchemaName(tenantId) + "." + table;
  }

  public int countRows(String tableName, String tenant) {
    var result = jdbcTemplate.queryForObject("SELECT count(*) FROM " + getTable(tenant, tableName), Integer.class);
    return result == null ? 0 : result;
  }

}

