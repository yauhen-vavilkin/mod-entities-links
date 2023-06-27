package org.folio.entlinks.integration.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SourceParsedContent {
  private final UUID id;
  private final String leader;
  private final List<FieldParsedContent> fields;
}
