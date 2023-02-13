package org.folio.entlinks.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("instance-storage")
public interface InstanceStorageClient {

  @GetMapping(value = "/instances", produces = APPLICATION_JSON_VALUE)
  InventoryInstanceDtoCollection getInstanceStorageInstances(@RequestParam String query, @RequestParam int limit);

  record InventoryInstanceDto(String id, String title) { }

  record InventoryInstanceDtoCollection(List<InventoryInstanceDto> instances) { }
}
