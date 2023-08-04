package org.folio.entlinks.controller;

import lombok.RequiredArgsConstructor;
import org.folio.entlinks.controller.delegate.suggestion.LinksSuggestionServiceDelegateHelper;
import org.folio.entlinks.domain.dto.AuthoritySearchParameter;
import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;
import org.folio.entlinks.rest.resource.LinksSuggestionsApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LinksSuggestionsController implements LinksSuggestionsApi {

  private final LinksSuggestionServiceDelegateHelper delegateServiceHelper;

  @Override
  public ResponseEntity<ParsedRecordContentCollection> suggestLinksForMarcRecord(
    ParsedRecordContentCollection parsedRecordContentCollection, AuthoritySearchParameter authoritySearchParameter,
    Boolean ignoreAutoLinkingEnabled) {
    return ResponseEntity.ok(
      delegateServiceHelper.getDelegate(authoritySearchParameter)
        .suggestLinksForMarcRecords(parsedRecordContentCollection, ignoreAutoLinkingEnabled)
    );
  }
}
