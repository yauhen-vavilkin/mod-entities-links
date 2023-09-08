package org.folio.entlinks.exception;

import lombok.Getter;
import org.folio.entlinks.exception.type.ErrorType;

/**
 * Base exception class that is used for all exceptional situations.
 */
@Getter
public abstract class BaseException extends RuntimeException {

  private final ErrorType errorType;

  /**
   * Initialize exception with provided message and error code.
   *
   * @param message   exception message
   * @param errorType exception code {@link ErrorType}
   */
  protected BaseException(String message, ErrorType errorType) {
    super(message);
    this.errorType = errorType;
  }

  /**
   * Initialize exception with provided message and error code.
   *
   * @param message   exception message
   * @param errorType exception code {@link ErrorType}
   * @param cause     cause Exception
   */
  protected BaseException(String message, ErrorType errorType, Throwable cause) {
    super(message, cause);
    this.errorType = errorType;
  }
}
