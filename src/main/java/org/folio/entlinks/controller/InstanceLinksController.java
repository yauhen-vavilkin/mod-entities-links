package org.folio.entlinks.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.service.InstanceLinkService;
import org.folio.qm.domain.dto.InstanceLinkDtoCollection;
import org.folio.qm.domain.dto.LinksCountDtoCollection;
import org.folio.qm.domain.dto.UuidCollection;
import org.folio.qm.rest.resource.InstanceLinksApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InstanceLinksController implements InstanceLinksApi {

  private final InstanceLinkService instanceLinkService;

  @Override
  public ResponseEntity<InstanceLinkDtoCollection> getInstanceLinks(UUID instanceId) {
    var links = instanceLinkService.getInstanceLinks(instanceId);
    return ResponseEntity.ok(links);
  }

  @Override
  public ResponseEntity<Void> updateInstanceLinks(UUID instanceId, InstanceLinkDtoCollection instanceLinkCollection) {
    instanceLinkService.updateInstanceLinks(instanceId, instanceLinkCollection);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<LinksCountDtoCollection> countLinksByAuthorityIds(UuidCollection authorityIdCollection){
    var linkCountMapDtoCollection = instanceLinkService.countLinksByAuthorityIds(authorityIdCollection);
    return ResponseEntity.ok(linkCountMapDtoCollection);
  }
}
