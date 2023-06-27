package org.folio.entlinks.integration.dto;

import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Getter
public class AuthorityParsedContent extends SourceParsedContent {
  private final String naturalId;

  public AuthorityParsedContent(UUID id, String naturalId, String leader,
                                List<FieldParsedContent> fields) {
    super(id, leader, fields);
    this.naturalId = naturalId;
  }
}
