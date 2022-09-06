package org.folio.entlinks.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.service.InstanceLinkService;
import org.folio.qm.domain.dto.InstanceLinkDtoCollection;
import org.folio.qm.rest.resource.InstanceLinksApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InstanceLinksController implements InstanceLinksApi {

  private final InstanceLinkService instanceLinkService;

  @Override
  public ResponseEntity<Void> updateInstanceLinks(UUID instanceId, InstanceLinkDtoCollection instanceLinkCollection) {
    instanceLinkService.updateInstanceLinks(instanceId, instanceLinkCollection);
    return ResponseEntity.noContent().build();
  }
}
