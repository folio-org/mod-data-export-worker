package org.folio.dew.error;

import static org.folio.dew.error.ErrorCode.IO_ERROR;
import static org.folio.dew.error.ErrorCode.NOT_FOUND_ERROR;
import static org.folio.dew.error.ErrorCode.VALIDATION_ERROR;
import static org.folio.dew.error.ErrorType.INTERNAL;

import org.folio.dew.domain.dto.Error;
import org.folio.dew.domain.dto.Errors;
import org.folio.dew.domain.dto.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;
import java.util.stream.Collectors;

@ControllerAdvice
public class DefaultErrorHandler {
  @ExceptionHandler(JobCommandNotFoundException.class)
  public ResponseEntity<Errors> handleJobNotFoundException(final JobCommandNotFoundException e) {
    return new ResponseEntity<>(new Errors()
      .errors(Collections.singletonList(new Error()
        .message(e.getMessage())
        .code(NOT_FOUND_ERROR.getDescription())
        .type(INTERNAL.getValue())))
      .totalRecords(1),
      HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(FileOperationException.class)
  public ResponseEntity<Errors> handleFileOperationException(final FileOperationException e) {
    return new ResponseEntity<>(new Errors()
      .errors(Collections.singletonList(new Error()
        .message(e.getMessage())
        .code(IO_ERROR.getDescription())
        .type(INTERNAL.getValue())))
      .totalRecords(1),
      HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(NonSupportedEntityTypeException.class)
  public ResponseEntity<Errors> handleNonSupportedEntityTypeException(final NonSupportedEntityTypeException e) {
    return new ResponseEntity<>(new Errors()
      .errors(Collections.singletonList(new Error()
        .message(e.getMessage())
        .code(VALIDATION_ERROR.getDescription())
        .type(INTERNAL.getValue())))
      .totalRecords(1),
      HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Errors> handleConstraintViolationException(final MethodArgumentNotValidException e) {
    var parameters = e.getBindingResult().getAllErrors().stream()
      .map(this::processValidationError)
      .collect(Collectors.toList());
    return new ResponseEntity<>(new Errors()
      .errors(Collections.singletonList(new Error()
        .message("Invalid request body")
        .parameters(parameters)
        .code(VALIDATION_ERROR.getDescription())
        .type(INTERNAL.getValue())))
      .totalRecords(1),
      HttpStatus.BAD_REQUEST);
  }

  private Parameter processValidationError(ObjectError error) {
    return new Parameter()
      .key(((FieldError) error).getField())
      .value(error.getDefaultMessage());
  }
}
