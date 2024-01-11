package org.folio.entlinks.exception;

import java.util.List;
import lombok.Getter;
import org.folio.entlinks.exception.type.ErrorType;
import org.folio.tenant.domain.dto.Parameter;

/**
 * Exception for situations when request body is invalid.
 */
@Getter
public class AuthoritiesRequestNotSupportedMediaTypeException extends BaseException {

  private final transient List<Parameter> invalidParameters;

  /**
   * Initialize exception with provided message, invalid media-types and invalidParameters.
   *
   * @param message   exception message
   * @param invalidParameters list of invalid parameters {@link Parameter}
   */
  public AuthoritiesRequestNotSupportedMediaTypeException(String message, List<Parameter> invalidParameters) {
    super(message, ErrorType.VALIDATION_ERROR);
    this.invalidParameters = invalidParameters;
  }

}
