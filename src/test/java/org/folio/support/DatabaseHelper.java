package org.folio.support;

import static java.lang.String.format;
import static org.folio.support.base.TestConstants.TENANT_ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.folio.entlinks.domain.entity.Authority;
import org.folio.entlinks.domain.entity.AuthorityData;
import org.folio.entlinks.domain.entity.AuthorityNoteType;
import org.folio.entlinks.domain.entity.AuthoritySourceFile;
import org.folio.entlinks.domain.entity.AuthoritySourceFileCode;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

public class DatabaseHelper {

  public static final String AUTHORITY_DATA_STAT_TABLE = "authority_data_stat";
  public static final String INSTANCE_AUTHORITY_LINK_TABLE = "instance_authority_link";
  public static final String AUTHORITY_DATA_TABLE = "authority_data";
  public static final String AUTHORITY_NOTE_TYPE_TABLE = "authority_note_type";
  public static final String AUTHORITY_SOURCE_FILE_TABLE = "authority_source_file";
  public static final String AUTHORITY_SOURCE_FILE_CODE_TABLE = "authority_source_file_code";
  public static final String AUTHORITY_TABLE = "authority";

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
    return JdbcTestUtils.countRowsInTable(jdbcTemplate, getTable(tenant, tableName));
  }

  public UUID getUuid(ResultSet rs) throws SQLException {
    var string = rs.getString("id");
    return string == null ? null : UUID.fromString(string);
  }

  public AuthorityData getAuthority(UUID id) {
    var table = getTable(TENANT_ID, AUTHORITY_DATA_TABLE);
    var sql = format("SELECT * FROM %s WHERE id = ?", table);
    return jdbcTemplate.query(sql, rs -> {
      rs.next();
      var data = new AuthorityData();
      data.setId(getUuid(rs));
      data.setDeleted(rs.getBoolean("state"));
      data.setNaturalId(rs.getString("natural_id"));
      return data;
    }, id);
  }

  public void saveAuthorityNoteType(String tenant, AuthorityNoteType entity) {
    var sql = "INSERT INTO " + getTable(tenant, AUTHORITY_NOTE_TYPE_TABLE)
      + " (id, name, source, created_date, updated_date, created_by_user_id, "
      + "updated_by_user_id) VALUES (?,?,?,?,?,?,?)";
    jdbcTemplate.update(sql, entity.getId(), entity.getName(), entity.getSource(), entity.getCreatedDate(),
      entity.getUpdatedDate(), entity.getCreatedByUserId(), entity.getUpdatedByUserId());
  }

  public void saveAuthoritySourceFile(String tenant, AuthoritySourceFile entity) {
    var sql = "INSERT INTO " + getTable(tenant, AUTHORITY_SOURCE_FILE_TABLE)
      + " (id, name, source, type, base_url, created_date, updated_date,"
      + "created_by_user_id, updated_by_user_id) VALUES (?,?,?,?,?,?,?,?,?)";
    jdbcTemplate.update(sql, entity.getId(), entity.getName(),
      entity.getSource(), entity.getType(), entity.getBaseUrl(), entity.getCreatedDate(),
      entity.getUpdatedDate(), entity.getCreatedByUserId(), entity.getUpdatedByUserId());
  }

  public void saveAuthoritySourceFileCode(String tenant, UUID sourceFileId, AuthoritySourceFileCode code) {
    var sql = "INSERT INTO " + getTable(tenant, AUTHORITY_SOURCE_FILE_CODE_TABLE)
      + " (authority_source_file_id, code) VALUES (?,?)";
    jdbcTemplate.update(sql, sourceFileId, code.getCode());
  }

  public void saveAuthority(String tenant, Authority entity) {
    var sql = "INSERT INTO " + getTable(tenant, AUTHORITY_TABLE)
        +  " (id, natural_id, source, heading, heading_type, subject_heading_code, created_date, "
        + "created_by_user_id, updated_date, updated_by_user_id) "
        + "VALUES (?,?,?,?,?,?,?,?,?,?)";
    jdbcTemplate.update(sql, entity.getId(), entity.getNaturalId(), entity.getSource(), entity.getHeading(),
        entity.getHeadingType(), entity.getSubjectHeadingCode(), entity.getCreatedDate(), entity.getCreatedByUserId(),
        entity.getUpdatedDate(), entity.getUpdatedByUserId());
  }

  public AuthorityNoteType getAuthorityNoteTypeById(UUID id, String tenant) {
    String sql = "SELECT * FROM " + getTable(tenant, AUTHORITY_NOTE_TYPE_TABLE) + " WHERE id = ?";
    return jdbcTemplate.query(sql, new Object[] {id}, rs -> {
      if (rs.next()) {
        var authorityNoteType = new AuthorityNoteType();
        authorityNoteType.setId(UUID.fromString(rs.getString("id")));
        authorityNoteType.setName(rs.getString("name"));
        authorityNoteType.setSource(rs.getString("source"));
        return authorityNoteType;
      } else {
        return null;
      }
    });
  }
}
