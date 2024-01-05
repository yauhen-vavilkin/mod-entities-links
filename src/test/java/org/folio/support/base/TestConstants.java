package org.folio.support.base;

import static org.folio.support.KafkaTestUtils.fullTopicName;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.entlinks.domain.dto.AuthoritySearchParameter;
import org.folio.entlinks.domain.dto.LinkAction;
import org.folio.entlinks.domain.dto.LinkStatus;

@UtilityClass
public class TestConstants {

  public static final String TENANT_ID = "test";
  public static final String CENTRAL_TENANT_ID = "consortium";
  public static final String USER_ID = "38d3a441-c100-5e8d-bd12-71bde492b723";
  public static final String SOURCE_FILE_NAME = "sourceFileName";
  public static final String SOURCE_FILE_TYPE = "sourceFileType";
  public static final String SOURCE_FILE_CODE = "sourceFileType";
  public static final String SOURCE_FILE_NATURAL_ID = "sourceFileNaturalId";
  public static final String SOURCE_FILE_SOURCE = "sourceFileSource";
  public static final String TEST_PROPERTY_VALUE = "value";
  public static final UUID TEST_ID = UUID.randomUUID();
  public static final int TEST_VERSION = 2;
  public static final Timestamp TEST_DATE = Timestamp.valueOf("2021-09-27 16:09:28.0");
  public static final String INPUT_BASE_URL = "https://www.id.loc.gov/authorities/test-source";

  public static final String INSTANCE_AUTHORITY_TOPIC = "links.instance-authority";
  public static final String INSTANCE_AUTHORITY_STATS_TOPIC = "links.instance-authority-stats";
  public static final String AUTHORITY_TOPIC = "authorities.authority";
  public static final String UPDATE_TYPE = "UPDATE";
  public static final String TEST_STRING = "test, ";

  private static final String INSTANCE_LINKS_ENDPOINT_PATH = "/links/instances/{id}";
  private static final String AUTHORITY_LINKS_COUNT_ENDPOINT_PATH = "/links/authorities/bulk/count";
  private static final String LINKS_SUGGESTIONS_ENDPOINT = "/links-suggestions/marc";
  private static final String LINKS_STATS_INSTANCE_ENDPOINT_PATH = "/links/stats/instance";
  private static final String LINKS_STATS_INSTANCE_ENDPOINT_PARAMS = "?status=%s&fromDate=%s&toDate=%s";
  private static final String AUTH_STATS_ENDPOINT_PATH_PATTERN = "/links/stats/authority";
  private static final String AUTH_STATS_ENDPOINT_PARAMS = "?action=%s&fromDate=%s&toDate=%s&limit=%d";
  private static final String LINKING_RULES_ENDPOINT = "/linking-rules/instance-authority";
  private static final String AUTHORITY_NOTE_TYPES_ENDPOINT = "/authority-note-types";
  private static final String AUTHORITY_SOURCE_FILES_ENDPOINT = "/authority-source-files";
  private static final String AUTHORITY_STORAGE_ENDPOINT = "/authority-storage/authorities";
  private static final String AUTHORITY_STORAGE_EXPIRE_ENDPOINT = "/authority-storage/expire/authorities";
  private static final String AUTHORITY_STORAGE_REINDEX_ENDPOINT = "/authority-storage/reindex";

  public static String authorityTopic() {
    return authorityTopic(TENANT_ID);
  }

  public static String authorityTopic(String tenantId) {
    return fullTopicName(AUTHORITY_TOPIC, tenantId);
  }

  public static String linksInstanceAuthorityTopic() {
    return fullTopicName(INSTANCE_AUTHORITY_TOPIC, TENANT_ID);
  }

  public static String linksInstanceAuthorityStatsTopic() {
    return fullTopicName(INSTANCE_AUTHORITY_STATS_TOPIC, TENANT_ID);
  }

  public static String linksInstanceEndpoint() {
    return INSTANCE_LINKS_ENDPOINT_PATH;
  }

  public static String linksSuggestionsEndpoint() {
    return LINKS_SUGGESTIONS_ENDPOINT;
  }

  public static String linksSuggestionsEndpoint(AuthoritySearchParameter authoritySearchParameter) {
    return LINKS_SUGGESTIONS_ENDPOINT + "?authoritySearchParameter=" + authoritySearchParameter;
  }

  public static String linksSuggestionsEndpoint(Boolean ignoreAutoLinkingEnabled) {
    return LINKS_SUGGESTIONS_ENDPOINT + "?ignoreAutoLinkingEnabled=" + ignoreAutoLinkingEnabled;
  }

  public static String linkingRulesEndpoint() {
    return LINKING_RULES_ENDPOINT;
  }

  public static String linkingRulesEndpoint(Integer id) {
    return LINKING_RULES_ENDPOINT + "/" + id;
  }

  public static String authoritiesLinksCountEndpoint() {
    return AUTHORITY_LINKS_COUNT_ENDPOINT_PATH;
  }

  public static String linksStatsInstanceEndpoint() {
    return LINKS_STATS_INSTANCE_ENDPOINT_PATH;
  }

  public static String linksStatsInstanceEndpoint(LinkStatus status, OffsetDateTime fromDate,
                                                  OffsetDateTime toDate) {
    return LINKS_STATS_INSTANCE_ENDPOINT_PATH
        + LINKS_STATS_INSTANCE_ENDPOINT_PARAMS.formatted(status, fromDate, toDate);
  }

  public static String authorityStatsEndpoint(LinkAction action, OffsetDateTime fromDate,
                                              OffsetDateTime toDate, int limit) {
    return AUTH_STATS_ENDPOINT_PATH_PATTERN + AUTH_STATS_ENDPOINT_PARAMS.formatted(action, fromDate, toDate, limit);
  }

  public static String authorityNoteTypesEndpoint() {
    return AUTHORITY_NOTE_TYPES_ENDPOINT;
  }

  public static String authorityNoteTypesEndpoint(UUID id) {
    return AUTHORITY_NOTE_TYPES_ENDPOINT + "/" + id;
  }

  public static String authoritySourceFilesEndpoint() {
    return AUTHORITY_SOURCE_FILES_ENDPOINT;
  }

  public static String authoritySourceFilesEndpoint(UUID id) {
    return AUTHORITY_SOURCE_FILES_ENDPOINT + "/" + id;
  }

  public static String authorityEndpoint() {
    return AUTHORITY_STORAGE_ENDPOINT;
  }

  public static String authorityEndpoint(UUID id) {
    return AUTHORITY_STORAGE_ENDPOINT + "/" + id;
  }

  public static String authorityExpireEndpoint() {
    return AUTHORITY_STORAGE_EXPIRE_ENDPOINT;
  }

  public static String authorityReindexEndpoint() {
    return AUTHORITY_STORAGE_REINDEX_ENDPOINT;
  }

  public static String authorityReindexEndpoint(UUID id) {
    return AUTHORITY_STORAGE_REINDEX_ENDPOINT + "/" + id;
  }
}
