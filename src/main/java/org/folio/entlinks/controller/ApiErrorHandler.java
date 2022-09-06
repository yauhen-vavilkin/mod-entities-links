package org.folio.entlinks.controller;

import static java.util.Collections.emptyList;
import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.WARN;
import static org.folio.entlinks.model.type.ErrorCode.UNKNOWN_ERROR;
import static org.folio.entlinks.model.type.ErrorCode.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.util.List;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.folio.entlinks.exception.RequestBodyValidationException;
import org.folio.entlinks.model.type.ErrorCode;
import org.folio.tenant.domain.dto.Error;
import org.folio.tenant.domain.dto.Errors;
import org.folio.tenant.domain.dto.Parameter;
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

  private static ResponseEntity<Errors> buildResponseEntity(Exception e, HttpStatus status, ErrorCode code) {
    var errors = new Errors()
      .errors(List.of(new Error()
        .message(e.getMessage())
        .type(e.getClass().getSimpleName())
        .code(code.getValue())))
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
    var error = new Error()
      .type(e.getClass().getSimpleName())
      .code(VALIDATION_ERROR.getValue())
      .message(e.getMessage())
      .parameters(parameters);
    return new Errors().errors(List.of(error)).totalRecords(1);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Errors> handleGlobalExceptions(Exception e) {
    logException(WARN, e);
    return buildResponseEntity(e, INTERNAL_SERVER_ERROR, UNKNOWN_ERROR);
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
      .map(error -> new Error()
        .message(error.getDefaultMessage())
        .code(VALIDATION_ERROR.getValue())
        .type(MethodArgumentNotValidException.class.getSimpleName())
        .addParametersItem(new Parameter()
          .key(((FieldError) error).getField())
          .value(String.valueOf(((FieldError) error).getRejectedValue()))))
      .toList();

    var errorResponse = new Errors().errors(errors).totalRecords(errors.size());
    return buildResponseEntity(errorResponse, UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Errors> handleConstraintViolation(ConstraintViolationException e) {
    logException(DEBUG, e);
    var errors = e.getConstraintViolations().stream()
      .map(constraintViolation -> new Error()
        .message(String.format("%s %s", constraintViolation.getPropertyPath(), constraintViolation.getMessage()))
        .code(VALIDATION_ERROR.getValue())
        .type(ConstraintViolationException.class.getSimpleName()))
      .toList();

    var errorResponse = new Errors().errors(errors).totalRecords(errors.size());
    return buildResponseEntity(errorResponse, BAD_REQUEST);
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

}
