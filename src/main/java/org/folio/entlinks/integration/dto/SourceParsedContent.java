package org.folio.entlinks.integration.dto;

import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SourceParsedContent {
  private final UUID id;
  private final String leader;
  private final Map<String, FieldParsedContent> fields;
}
