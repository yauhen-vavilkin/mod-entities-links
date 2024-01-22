package org.folio.entlinks.controller;

import static org.folio.entlinks.exception.type.ErrorType.VALIDATION_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.entlinks.exception.AuthoritiesRequestNotSupportedMediaTypeException;
import org.folio.entlinks.exception.AuthorityNotFoundException;
import org.folio.entlinks.exception.AuthorityNoteTypeNotFoundException;
import org.folio.entlinks.exception.AuthoritySourceFileHridException;
import org.folio.entlinks.exception.AuthoritySourceFileNotFoundException;
import org.folio.entlinks.exception.LinkingRuleNotFoundException;
import org.folio.entlinks.exception.MarcAuthorityNotFoundException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.entlinks.exception.ReindexJobNotFoundException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.exception.ResourceNotFoundException;
import org.folio.entlinks.exception.type.ErrorType;
import org.folio.tenant.domain.dto.Errors;
import org.folio.tenant.domain.dto.Parameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


class ApiErrorHandlerTest {
  private static final String TEST_ERROR_MESSAGE = "Test error message";
  private static final String FIELD_NAME = "fieldName";
  private static final String INVALID_VALUE = "invalidValue";

  private ApiErrorHandler apiErrorHandler;

  @BeforeEach
  void setUp() {
    apiErrorHandler = new ApiErrorHandler();
  }

  @Test
  void handleGlobalExceptions_ReturnsInternalServerErrorResponse() {
    // Arrange
    Exception exception = mock(Exception.class);
    when(exception.getMessage()).thenReturn(TEST_ERROR_MESSAGE);

    // Act
    ResponseEntity<Errors> responseEntity = apiErrorHandler.handleGlobalExceptions(exception);

    // Assert
    assertErrorResponse(exception, ErrorType.UNKNOWN_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, responseEntity,
        TEST_ERROR_MESSAGE);
  }

  @ParameterizedTest
  @MethodSource("resourceNotFoundExceptionsProvider")
  void handleResourceNotFoundException_ReturnsNotFoundResponse(ResourceNotFoundException exception) {
    // Act
    ResponseEntity<Errors> responseEntity = apiErrorHandler.handleNotFoundException(exception);

    // Assert
    assertErrorResponse(exception, ErrorType.NOT_FOUND_ERROR, HttpStatus.NOT_FOUND, responseEntity,
        exception.getMessage());
  }

  @Test
  void handleOptimisticLockingException_ReturnsConflictResponse() {
    // Arrange
    UUID recordId = UUID.randomUUID();
    int existingVersion = 1;
    int requestVersion = 2;

    var exception = OptimisticLockingException.optimisticLockingOnUpdate(recordId, existingVersion, requestVersion);

    // Act
    ResponseEntity<Errors> responseEntity = apiErrorHandler.handleOptimisticLockingException(exception);

    // Verify the specific message content based on the static factory method
    String expectedErrorMessage = String.format("Cannot update record %s because it has been changed "
            + "(optimistic locking): Stored _version is %d, _version of request is %d",
        recordId, existingVersion, requestVersion);

    // Assert
    assertErrorResponse(exception, ErrorType.OPTIMISTIC_LOCKING_ERROR, HttpStatus.CONFLICT, responseEntity,
        expectedErrorMessage);
  }

  @Test
  void handleAuthoritySourceFileHridException_ReturnsResponseEntity() {
    // Arrange
    var exception = mock(AuthoritySourceFileHridException.class);
    when(exception.getMessage()).thenReturn(TEST_ERROR_MESSAGE);
    when(exception.getErrorType()).thenReturn(VALIDATION_ERROR);

    // Act
    ResponseEntity<Errors> responseEntity = apiErrorHandler.handleAuthoritySourceFileHridException(exception);

    // Assert
    assertErrorResponse(exception, VALIDATION_ERROR, HttpStatus.UNPROCESSABLE_ENTITY, responseEntity,
        TEST_ERROR_MESSAGE);
  }

  @Test
  void handleRequestValidationException_ReturnsUnprocessableEntityResponse() {
    var exception = new RequestBodyValidationException(TEST_ERROR_MESSAGE, createInvalidParameter());

    // Act
    ResponseEntity<Errors> responseEntity = apiErrorHandler.handleRequestValidationException(exception);

    // Assert
    assertErrorResponse(exception, VALIDATION_ERROR, HttpStatus.UNPROCESSABLE_ENTITY, responseEntity,
        TEST_ERROR_MESSAGE);

    var error = Objects.requireNonNull(responseEntity.getBody()).getErrors().get(0);

    // Verify invalid parameters in the response
    assertNotNull(error.getParameters());
    assertEquals(1, error.getParameters().size());

    Parameter expectedParameter = error.getParameters().get(0);
    assertEquals(FIELD_NAME, expectedParameter.getKey());
    assertEquals(INVALID_VALUE, expectedParameter.getValue());
  }

  @Test
  void handleAuthoritiesRequestNotSupportedMediaTypeException_ReturnsBadRequestResponse() {
    var exception = new AuthoritiesRequestNotSupportedMediaTypeException(TEST_ERROR_MESSAGE, createInvalidParameter());

    // Act
    ResponseEntity<String> responseEntity =
        apiErrorHandler.handleAuthoritiesMediaTypeValidationException(exception);

    // Assert
    assertNotNull(responseEntity);
    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

    HttpHeaders headers = responseEntity.getHeaders();
    assertNotNull(headers);
    assertEquals(MediaType.TEXT_PLAIN, headers.getContentType());

    String responseBody = responseEntity.getBody();
    assertNotNull(responseBody);

    String expectedErrorResponse = "message: " + TEST_ERROR_MESSAGE + System.lineSeparator()
        + "type: AuthoritiesRequestNotSupportedMediaTypeException" + System.lineSeparator()
        + "code: validation" + System.lineSeparator()
        + "parameters: [(key: " + FIELD_NAME + ", value: " + INVALID_VALUE + ")]";

    assertEquals(expectedErrorResponse, responseBody);
  }

  @Test
  void handleMethodArgumentNotValidException_ReturnsUnprocessableEntityResponse() {
    // Arrange
    FieldError fieldError = new FieldError("objectName", FIELD_NAME, TEST_ERROR_MESSAGE);
    BindingResult bindingResult = mock(BindingResult.class);
    when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));

    var exception = new MethodArgumentNotValidException(null, bindingResult);

    // Act
    ResponseEntity<Errors> responseEntity =
        apiErrorHandler.handleMethodArgumentNotValidException(exception);

    // Assert
    assertErrorResponse(exception, VALIDATION_ERROR, HttpStatus.UNPROCESSABLE_ENTITY, responseEntity,
        TEST_ERROR_MESSAGE);

    var error = Objects.requireNonNull(responseEntity.getBody()).getErrors().get(0);

    List<Parameter> parameters = error.getParameters();
    assertNotNull(parameters);
    assertEquals(1, parameters.size());

    Parameter parameter = parameters.get(0);
    assertEquals(FIELD_NAME, parameter.getKey());
    assertEquals("null", parameter.getValue());
  }

  @ParameterizedTest
  @MethodSource("validationExceptionsProvider")
  void handleValidationException(Exception exception) {
    // Act
    ResponseEntity<Errors> responseEntity = apiErrorHandler.handleValidationException(exception);

    // Assert
    assertErrorResponse(exception, VALIDATION_ERROR, HttpStatus.BAD_REQUEST, responseEntity, exception.getMessage());
  }

  @Test
  void handleHttpMessageNotReadableException_WithValidationCause_ReturnsValidationErrorResponse() {
    // Arrange
    var validationException = new IllegalArgumentException(TEST_ERROR_MESSAGE);
    var httpMessageNotReadableException = new HttpMessageNotReadableException(TEST_ERROR_MESSAGE, validationException);

    // Act
    ResponseEntity<Errors> responseEntity =
        apiErrorHandler.handlerHttpMessageNotReadableException(httpMessageNotReadableException);

    // Assert
    assertErrorResponse(httpMessageNotReadableException, VALIDATION_ERROR, HttpStatus.BAD_REQUEST, responseEntity,
        TEST_ERROR_MESSAGE);
  }

  @ParameterizedTest
  @MethodSource("violationExceptionsProvider")
  void handleViolationException(Exception exception) {
    // Act
    ResponseEntity<Errors> responseEntity = apiErrorHandler.conflict(exception);

    // Assert
    assertErrorResponse(exception, VALIDATION_ERROR, HttpStatus.BAD_REQUEST, responseEntity, exception.getMessage());
  }

  private static List<Parameter> createInvalidParameter() {
    var parameter = new Parameter(FIELD_NAME);
    parameter.setValue(INVALID_VALUE);
    return Collections.singletonList(parameter);
  }

  private static Stream<Exception> resourceNotFoundExceptionsProvider() {
    return Stream.of(
        mock(AuthorityNotFoundException.class),
        mock(AuthorityNoteTypeNotFoundException.class),
        mock(AuthoritySourceFileNotFoundException.class),
        mock(LinkingRuleNotFoundException.class),
        mock(MarcAuthorityNotFoundException.class),
        mock(ReindexJobNotFoundException.class)
    );
  }

  private static Stream<Exception> validationExceptionsProvider() {
    return Stream.of(
        mock(MethodArgumentTypeMismatchException.class),
        mock(MissingServletRequestParameterException.class),
        mock(IllegalArgumentException.class)
    );
  }

  private static Stream<Exception> violationExceptionsProvider() {
    return Stream.of(
        mock(DataIntegrityViolationException.class),
        mock(InvalidDataAccessApiUsageException.class)
    );
  }

  private static void assertErrorResponse(Exception exception, ErrorType errorType, HttpStatus expectedStatus,
                                          ResponseEntity<Errors> responseEntity, String errorMessage) {
    assertNotNull(responseEntity);
    assertEquals(expectedStatus, responseEntity.getStatusCode());

    Errors errors = responseEntity.getBody();
    assertNotNull(errors);
    assertEquals(1, errors.getTotalRecords());
    assertEquals(1, errors.getErrors().size());

    var error = errors.getErrors().get(0);
    assertEquals(errorMessage, error.getMessage());
    assertEquals(exception.getClass().getSimpleName(), error.getType());
    assertEquals(errorType.getValue(), error.getCode());
  }
}
