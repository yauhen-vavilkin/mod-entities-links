package org.folio.entlinks.controller;

import static java.util.Collections.emptyList;
import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.WARN;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_ID;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_SOURCE_FILE_CODE;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_SOURCE_FILE_ID;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_SOURCE_FILE_NAME;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_SOURCE_FILE_SEQUENCE;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_AUTHORITY_SOURCE_FILE_URL;
import static org.folio.entlinks.config.constants.ErrorCode.DUPLICATE_NOTE_TYPE_NAME;
import static org.folio.entlinks.config.constants.ErrorCode.VIOLATION_OF_RELATION_BETWEEN_AUTHORITY_AND_SOURCE_FILE;
import static org.folio.entlinks.exception.type.ErrorType.UNKNOWN_ERROR;
import static org.folio.entlinks.exception.type.ErrorType.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.folio.entlinks.config.constants.ErrorCode;
import org.folio.entlinks.exception.AuthoritySourceFileHridException;
import org.folio.entlinks.exception.OptimisticLockingException;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.exception.ResourceNotFoundException;
import org.folio.entlinks.exception.type.ErrorType;
import org.folio.tenant.domain.dto.Error;
import org.folio.tenant.domain.dto.Errors;
import org.folio.tenant.domain.dto.Parameter;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Log4j2
@RestControllerAdvice
public class ApiErrorHandler {

  private static final Map<String, ErrorCode> CONSTRAINS_I18N_MAP = Map.of(
    "authority_note_type_name_unq", DUPLICATE_NOTE_TYPE_NAME,
    "authority_source_file_name_unq", DUPLICATE_AUTHORITY_SOURCE_FILE_NAME,
    "authority_source_file_base_url_unq", DUPLICATE_AUTHORITY_SOURCE_FILE_URL,
    "authority_source_file_code_unq", DUPLICATE_AUTHORITY_SOURCE_FILE_CODE,
    "pk_authority_storage", DUPLICATE_AUTHORITY_ID,
    "authority_storage_source_file_id_foreign_key", VIOLATION_OF_RELATION_BETWEEN_AUTHORITY_AND_SOURCE_FILE,
    "authority_source_file_sequence_name_unq", DUPLICATE_AUTHORITY_SOURCE_FILE_SEQUENCE,
    "pk_authority_source_file", DUPLICATE_AUTHORITY_SOURCE_FILE_ID);

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Errors> handleGlobalExceptions(Exception e) {
    logException(WARN, e);
    return buildResponseEntity(e, INTERNAL_SERVER_ERROR, UNKNOWN_ERROR);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Errors> handleNotFoundException(ResourceNotFoundException e) {
    return buildResponseEntity(e, NOT_FOUND, ErrorType.NOT_FOUND_ERROR);
  }

  @ExceptionHandler(OptimisticLockingException.class)
  public ResponseEntity<Errors> handleOptimisticLockingException(OptimisticLockingException e) {
    return buildResponseEntity(e, HttpStatus.CONFLICT, ErrorType.OPTIMISTIC_LOCKING_ERROR);
  }

  @ExceptionHandler(AuthoritySourceFileHridException.class)
  public ResponseEntity<Errors> handleAuthoritySourceFileHridException(AuthoritySourceFileHridException e) {
    return buildResponseEntity(e, UNPROCESSABLE_ENTITY, e.getErrorType());
  }

  @ExceptionHandler(RequestBodyValidationException.class)
  public ResponseEntity<Errors> handleRequestValidationException(RequestBodyValidationException e) {
    logException(DEBUG, e);
    var errorResponse = buildValidationError(e, e.getInvalidParameters());
    return buildResponseEntity(errorResponse, UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Errors> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    logException(DEBUG, e);
    var errors = Optional.of(e.getBindingResult())
      .map(org.springframework.validation.Errors::getAllErrors)
      .orElse(emptyList())
      .stream()
      .map(error -> new Error(error.getDefaultMessage())
        .code(VALIDATION_ERROR.getValue())
        .type(MethodArgumentNotValidException.class.getSimpleName())
        .addParametersItem(new Parameter(((FieldError) error).getField())
          .value(String.valueOf(((FieldError) error).getRejectedValue()))))
      .toList();

    var errorResponse = new Errors().errors(errors).totalRecords(errors.size());
    return buildResponseEntity(errorResponse, UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<Errors> handleValidationException(Exception e) {
    logException(DEBUG, e);
    return buildResponseEntity(e, BAD_REQUEST, VALIDATION_ERROR);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Errors> handlerHttpMessageNotReadableException(HttpMessageNotReadableException e) {
    return Optional.ofNullable(e.getCause())
      .map(Throwable::getCause)
      .filter(IllegalArgumentException.class::isInstance)
      .map(IllegalArgumentException.class::cast)
      .map(this::handleValidationException)
      .orElseGet(() -> {
        logException(DEBUG, e);
        return buildResponseEntity(e, BAD_REQUEST, VALIDATION_ERROR);
      });
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Errors> conflict(DataIntegrityViolationException e) {
    var cause = e.getCause();
    if (cause instanceof ConstraintViolationException cve) {
      var constraintName = cve.getConstraintName();
      var errorCode = CONSTRAINS_I18N_MAP.get(constraintName);
      return buildResponseEntity(errorCode, VALIDATION_ERROR, UNPROCESSABLE_ENTITY);
    }
    return buildResponseEntity(e, BAD_REQUEST, VALIDATION_ERROR);
  }

  private static ResponseEntity<Errors> buildResponseEntity(Exception e, HttpStatus status, ErrorType type) {
    var errors = new Errors()
      .errors(List.of(new Error(e.getMessage())
        .type(e.getClass().getSimpleName())
        .code(type != null ? type.getValue() : null)))
      .totalRecords(1);
    return buildResponseEntity(errors, status);
  }

  private static ResponseEntity<Errors> buildResponseEntity(ErrorCode errorCode, ErrorType type, HttpStatus status) {
    var errors = new Errors()
      .errors(List.of(new Error(errorCode.getMessage())
        .type(type.getValue())
        .code(errorCode.getCode())))
      .totalRecords(1);
    return buildResponseEntity(errors, status);
  }

  private static ResponseEntity<Errors> buildResponseEntity(Errors errorResponse, HttpStatus status) {
    return ResponseEntity.status(status).body(errorResponse);
  }

  private static void logException(Level logLevel, Exception e) {
    log.log(logLevel, "Handling e", e);
  }

  private static Errors buildValidationError(Exception e, List<Parameter> parameters) {
    var error = new Error(e.getMessage())
      .type(e.getClass().getSimpleName())
      .code(VALIDATION_ERROR.getValue())
      .parameters(parameters);
    return new Errors().errors(List.of(error)).totalRecords(1);
  }

}
