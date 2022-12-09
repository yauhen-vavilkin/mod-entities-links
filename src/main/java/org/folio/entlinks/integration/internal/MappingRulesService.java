package org.folio.entlinks.integration.internal;

import static org.folio.entlinks.config.constants.CacheNames.AUTHORITY_MAPPING_RULES_CACHE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.MappingRulesClient;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class MappingRulesService {

  private final MappingRulesClient client;

  @Cacheable(cacheNames = AUTHORITY_MAPPING_RULES_CACHE,
             key = "@folioExecutionContext.tenantId",
             unless = "#result.isEmpty()")
  public Map<String, List<String>> getFieldTargetsMappingRelations() {
    log.info("Fetching authority mapping rules");
    var mappingRules = fetchMappingRules();
    return mappingRules.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, rulesList ->
        rulesList.getValue().stream().map(MappingRulesClient.MappingRule::target).toList()));
  }

  private Map<String, List<MappingRulesClient.MappingRule>> fetchMappingRules() {
    try {
      return client.fetchAuthorityMappingRules();
    } catch (Exception e) {
      throw new FolioIntegrationException("Failed to fetch authority mapping rules", e);
    }
  }
}
