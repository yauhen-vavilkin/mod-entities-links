package org.folio.entlinks.client;

import java.util.UUID;
import org.folio.entlinks.domain.dto.SourceRecord;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("source-storage")
public interface SourceStorageClient {

  @GetMapping("/source-records/{id}?idType=AUTHORITY")
  SourceRecord getMarcAuthorityById(@PathVariable("id") UUID id);
}
