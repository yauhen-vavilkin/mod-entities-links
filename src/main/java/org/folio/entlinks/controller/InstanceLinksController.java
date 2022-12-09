package org.folio.entlinks.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.LinkingServiceDelegate;
import org.folio.entlinks.domain.dto.InstanceLinkDtoCollection;
import org.folio.entlinks.domain.dto.LinksCountDtoCollection;
import org.folio.entlinks.domain.dto.UuidCollection;
import org.folio.entlinks.rest.resource.InstanceLinksApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InstanceLinksController implements InstanceLinksApi {

  private final LinkingServiceDelegate linkingServiceDelegate;

  @Override
  public ResponseEntity<LinksCountDtoCollection> countLinksByAuthorityIds(UuidCollection authorityIdCollection) {
    var counts = linkingServiceDelegate.countLinksByAuthorityIds(authorityIdCollection);
    return ResponseEntity.ok(counts);
  }

  @Override
  public ResponseEntity<InstanceLinkDtoCollection> getInstanceLinks(UUID instanceId) {
    var links = linkingServiceDelegate.getLinks(instanceId);
    return ResponseEntity.ok(links);
  }

  @Override
  public ResponseEntity<Void> updateInstanceLinks(UUID instanceId, InstanceLinkDtoCollection instanceLinkCollection) {
    linkingServiceDelegate.updateLinks(instanceId, instanceLinkCollection);
    return ResponseEntity.noContent().build();
  }
}
