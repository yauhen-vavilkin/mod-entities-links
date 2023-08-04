package org.folio.entlinks.controller.delegate.suggestion;

import org.folio.entlinks.domain.dto.ParsedRecordContentCollection;

public interface LinksSuggestionServiceDelegate {
  ParsedRecordContentCollection suggestLinksForMarcRecords(
      ParsedRecordContentCollection contentCollection, Boolean ignoreAutoLinkingEnabled);
}
