package org.folio.entlinks.integration.internal;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.entlinks.client.InstanceStorageClient;
import org.folio.entlinks.client.InstanceStorageClient.InventoryInstanceDto;
import org.folio.entlinks.client.InstanceStorageClient.InventoryInstanceDtoCollection;
import org.folio.entlinks.config.properties.InstanceStorageProperties;
import org.folio.entlinks.exception.FolioIntegrationException;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceStorageService {

  private static final String CQL_TEMPLATE = "id==(%s)";
  private static final String CQL_DELIMITER = " or ";
  private final InstanceStorageProperties instanceStorageProperties;
  private final InstanceStorageClient client;

  public Map<String, String> getInstanceTitles(List<String> instanceIds) {
    int batchSize = instanceStorageProperties.getBatchSize();
    log.info("Fetching instance titles [count: {}, with batch size: {}]", instanceIds.size(), batchSize);
    log.trace("Fetching instance titles for [instance ids: {}]", instanceIds);
    return Lists.partition(instanceIds, batchSize).stream()
      .map(ids -> fetchInstances(buildCql(ids), ids.size()).instances())
      .flatMap(Collection::stream)
      .collect(Collectors.toMap(InventoryInstanceDto::id, InventoryInstanceDto::title));
  }

  private String buildCql(List<String> instanceIds) {
    var instanceIdsString = String.join(CQL_DELIMITER, instanceIds);
    return String.format(CQL_TEMPLATE, instanceIdsString);
  }

  private InventoryInstanceDtoCollection fetchInstances(String query, int limit) {
    try {
      log.info("Fetching instances for query: {}, limit: {}", query, limit);
      return client.getInstanceStorageInstances(query, limit);
    } catch (Exception e) {
      throw new FolioIntegrationException("Failed to fetch instances", e);
    }
  }
}
