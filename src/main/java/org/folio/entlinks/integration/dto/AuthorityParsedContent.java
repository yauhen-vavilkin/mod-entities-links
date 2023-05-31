package org.folio.entlinks.integration.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
public class AuthorityParsedContent extends SourceParsedContent {
  private final String naturalId;

  public AuthorityParsedContent(UUID id, String naturalId, String leader, Map<String, FieldParsedContent> fields) {
    super(id, leader, fields);
    this.naturalId = naturalId;
  }
}
