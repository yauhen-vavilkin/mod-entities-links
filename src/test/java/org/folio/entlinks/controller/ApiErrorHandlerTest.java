package org.folio.entlinks.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.folio.entlinks.exception.AuthoritySourceFileHridException;
import org.folio.entlinks.exception.type.ErrorType;
import org.folio.tenant.domain.dto.Errors;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiErrorHandlerTest {

  @Test
  void handleAuthoritySourceFileHridException_ReturnsResponseEntity() {
    // Arrange
    AuthoritySourceFileHridException exception = mock(AuthoritySourceFileHridException.class);
    when(exception.getMessage()).thenReturn("Test error message");
    when(exception.getErrorType()).thenReturn(ErrorType.VALIDATION_ERROR);

    ApiErrorHandler yourController = new ApiErrorHandler();

    // Act
    ResponseEntity<Errors> responseEntity = yourController.handleAuthoritySourceFileHridException(exception);

    // Assert
    assertNotNull(responseEntity);
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());

    Errors errors = responseEntity.getBody();
    assertNotNull(errors);
    assertEquals(1, errors.getTotalRecords());
    assertEquals(1, errors.getErrors().size());

    var error = errors.getErrors().get(0);
    assertEquals("Test error message", error.getMessage());
    assertEquals(exception.getClass().getSimpleName(), error.getType());
    assertEquals(ErrorType.VALIDATION_ERROR.getValue(), error.getCode());
  }

}
