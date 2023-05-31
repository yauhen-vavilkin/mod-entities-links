package org.folio.entlinks.controller;

import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.LinksSuggestionsServiceDelegate;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.rest.resource.LinksSuggestionsApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LinksSuggestionsController implements LinksSuggestionsApi {

  private final LinksSuggestionsServiceDelegate serviceDelegate;

  @Override
  public ResponseEntity<ParsedRecordContentCollection> suggestLinksForMarcRecord(
    ParsedRecordContentCollection parsedRecordContentCollection) {
    return ResponseEntity.ok(
      serviceDelegate.suggestLinksForMarcRecords(parsedRecordContentCollection)
    );
  }
}
