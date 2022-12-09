package org.folio.entlinks.exception;

import java.util.UUID;
import lombok.Getter;

public class AuthorityBatchProcessingException extends Exception {

  @Getter
  private final UUID authorityId;

  public AuthorityBatchProcessingException(String message) {
    this(null, message);
  }

  public AuthorityBatchProcessingException(UUID authorityId, String message) {
    super(message);
    this.authorityId = authorityId;
  }
}
