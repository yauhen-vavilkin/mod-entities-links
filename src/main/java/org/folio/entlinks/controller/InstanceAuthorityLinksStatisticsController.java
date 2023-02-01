package org.folio.entlinks.controller;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.InstanceAuthorityStatServiceDelegate;
import org.folio.entlinks.domain.dto.AuthorityChangeStatDtoCollection;
import org.folio.entlinks.domain.dto.AuthorityDataStatActionDto;
import org.folio.entlinks.rest.resource.InstanceAuthorityLinksStatisticsApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InstanceAuthorityLinksStatisticsController implements InstanceAuthorityLinksStatisticsApi {

  private final InstanceAuthorityStatServiceDelegate delegate;

  @Override
  public ResponseEntity<AuthorityChangeStatDtoCollection> getAuthorityLinksStats(OffsetDateTime fromDate,
                                                                                 OffsetDateTime toDate,
                                                                                 AuthorityDataStatActionDto action,
                                                                                 Integer limit) {
    return ResponseEntity.ok(delegate.fetchAuthorityLinksStats(fromDate, toDate, action, limit));
  }

}

