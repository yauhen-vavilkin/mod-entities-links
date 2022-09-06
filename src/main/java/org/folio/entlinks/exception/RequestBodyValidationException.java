package org.folio.entlinks.exception;

import java.util.List;
import lombok.Getter;
import org.folio.entlinks.model.type.ErrorCode;
import org.folio.tenant.domain.dto.Parameter;

/**
 * Exception for situations when request body is invalid
 */
@Getter
public class RequestBodyValidationException extends BaseException {

  private final transient List<Parameter> invalidParameters;

  /**
   * Initialize exception with provided message, error code and invalid parameters.
   *
   * @param message   exception message
   * @param invalidParameters list of invalid parameters {@link Parameter}
   */
  public RequestBodyValidationException(String message, List<Parameter> invalidParameters) {
    super(message, ErrorCode.VALIDATION_ERROR);
    this.invalidParameters = invalidParameters;
  }

}
