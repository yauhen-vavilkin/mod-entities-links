package org.folio.entlinks.exception;

import static org.folio.entlinks.exception.type.ErrorType.UNKNOWN_ERROR;
import static org.folio.entlinks.exception.type.ErrorType.VALIDATION_ERROR;

import java.util.UUID;

public class AuthoritySourceFileHridException extends BaseException {

  private static final String ERROR_MESSAGE = "Cannot get next HRID for source file [id: %s, cause: %s]";
  private static final String SUPPORT_HRID_GENERATION_CAUSE = "source file doesn't support HRID generation";

  public AuthoritySourceFileHridException(UUID sourceFileId) {
    super(ERROR_MESSAGE.formatted(sourceFileId, SUPPORT_HRID_GENERATION_CAUSE), VALIDATION_ERROR);
  }

  public AuthoritySourceFileHridException(UUID sourceFileId, Throwable cause) {
    super(ERROR_MESSAGE.formatted(sourceFileId, cause.getMessage()), UNKNOWN_ERROR, cause);
  }
}
