package org.folio.entlinks.controller;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.InstanceAuthorityStatServiceDelegate;
import org.folio.entlinks.controller.delegate.LinkingServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityStatsDtoCollection;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
import org.folio.entlinks.domain.dto.LinkAction;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.rest.resource.InstanceAuthorityLinksStatisticsApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InstanceAuthorityLinksStatisticsController implements InstanceAuthorityLinksStatisticsApi {
  private final InstanceAuthorityStatServiceDelegate instanceAuthorityStatServiceDelegate;
  private final LinkingServiceDelegate linkingServiceDelegate;

  @Override
  public ResponseEntity<AuthorityStatsDtoCollection> getAuthorityLinksStats(OffsetDateTime fromDate,
                                                                            OffsetDateTime toDate,
                                                                            LinkAction action, Integer limit) {
    return ResponseEntity.ok(
      instanceAuthorityStatServiceDelegate.fetchAuthorityLinksStats(fromDate, toDate, action, limit)
    );
  }

  @Override
  public ResponseEntity<BibStatsDtoCollection> getLinkedBibUpdateStats(OffsetDateTime fromDate,
                                                                       OffsetDateTime toDate,
                                                                       LinkStatus status, Integer limit) {
    return ResponseEntity.ok(
      linkingServiceDelegate.getLinkedBibUpdateStats(fromDate, toDate, status, limit)
    );
  }
}

