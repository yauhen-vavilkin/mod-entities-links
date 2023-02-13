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
import org.folio.entlinks.exception.FolioIntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceStorageService {

  private static final String CQL_TEMPLATE = "id==(%s)";
  private static final String CQL_DELIMITER = " or ";

  @Value("${folio.instance-storage.batch-size:100}")
  private int instanceBatchSize;

  private final InstanceStorageClient client;

  public Map<String, String> getInstanceTitles(List<String> instanceIds) {
    log.info("Fetching instance titles [count: {}, with batch size: {}]", instanceIds.size(), instanceBatchSize);
    log.trace("Fetching instance titles for [instance ids: {}]", instanceIds);
    return Lists.partition(instanceIds, instanceBatchSize).stream()
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
