package org.folio.support.base;

import static org.folio.support.KafkaTestUtils.fullTopicName;

import java.time.OffsetDateTime;
import lombok.experimental.UtilityClass;
import org.folio.entlinks.domain.dto.LinkAction;
import org.folio.entlinks.domain.dto.LinkStatus;

@UtilityClass
public class TestConstants {

  public static final String TENANT_ID = "test";
  public static final String USER_ID = "38d3a441-c100-5e8d-bd12-71bde492b723";

  public static final String AUTHORITY_TOPIC = "inventory.authority";
  public static final String INSTANCE_AUTHORITY_TOPIC = "links.instance-authority";
  public static final String INSTANCE_AUTHORITY_STATS_TOPIC = "links.instance-authority-stats";
  public static final String DELETE_TYPE = "DELETE";
  public static final String UPDATE_TYPE = "UPDATE";

  private static final String INSTANCE_LINKS_ENDPOINT_PATH = "/links/instances/{id}";
  private static final String AUTHORITY_LINKS_COUNT_ENDPOINT_PATH = "/links/authorities/bulk/count";
  private static final String LINKS_SUGGESTIONS_ENDPOINT = "/links-suggestions/marc";
  private static final String LINKS_STATS_INSTANCE_ENDPOINT_PATH = "/links/stats/instance";
  private static final String LINKS_STATS_INSTANCE_ENDPOINT_PARAMS = "?status=%s&fromDate=%s&toDate=%s";
  private static final String AUTH_STATS_ENDPOINT_PATH_PATTERN = "/links/stats/authority";
  private static final String AUTH_STATS_ENDPOINT_PARAMS = "?action=%s&fromDate=%s&toDate=%s&limit=%d";
  private static final String LINKING_RULES_ENDPOINT = "/linking-rules/instance-authority";

  public static String inventoryAuthorityTopic() {
    return fullTopicName(AUTHORITY_TOPIC, TENANT_ID);
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
}
