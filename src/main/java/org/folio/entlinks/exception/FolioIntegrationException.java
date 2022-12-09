package org.folio.entlinks.exception;

import static org.folio.entlinks.exception.type.ErrorCode.INTEGRATION_ERROR;

/**
 * Handles exceptional cases of module integration with other Folio modules.
 */
public class FolioIntegrationException extends BaseException {

  /**
   * Initialize exception with provided message and error code.
   *
   * @param message exception message
   */
  public FolioIntegrationException(String message) {
    super(message, INTEGRATION_ERROR);
  }

  /**
   * Initialize exception with provided message and error code.
   *
   * @param message exception message
   * @param cause   cause Exception
   */
  public FolioIntegrationException(String message, Throwable cause) {
    super(message, INTEGRATION_ERROR, cause);
  }
}
