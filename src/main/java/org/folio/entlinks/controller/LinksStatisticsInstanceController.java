package org.folio.entlinks.controller;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.LinkingServiceDelegate;
import org.folio.entlinks.domain.dto.BibStatsDtoCollection;
import org.folio.entlinks.domain.dto.LinkStatus;
import org.folio.entlinks.rest.resource.LinkedBibUpdateStatisticsApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LinksStatisticsInstanceController implements LinkedBibUpdateStatisticsApi {

  private final LinkingServiceDelegate delegate;

  @Override
  public ResponseEntity<BibStatsDtoCollection> getLinkedBibUpdateStats(OffsetDateTime fromDate, OffsetDateTime toDate,
                                                                       LinkStatus status, Integer limit) {
    return ResponseEntity.ok(delegate.getLinkedBibUpdateStats(status, fromDate, toDate, limit));
  }
}

