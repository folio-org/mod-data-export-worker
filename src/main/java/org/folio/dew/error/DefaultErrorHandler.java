package org.folio.dew.error;

import static org.folio.dew.error.ErrorCode.IO_ERROR;
import static org.folio.dew.error.ErrorCode.NOT_FOUND_ERROR;
import static org.folio.dew.error.ErrorCode.PROCESSING_ERROR;
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
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Errors> handleNotFoundException(final NotFoundException e) {
    return new ResponseEntity<>(new Errors()
      .errors(Collections.singletonList(new Error()
        .message(e.getMessage())
        .code(NOT_FOUND_ERROR.getDescription())
        .type(org.folio.dew.domain.dto.ErrorType.ERROR)))
      .totalRecords(1),
      HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(FileOperationException.class)
  public ResponseEntity<Errors> handleFileOperationException(final FileOperationException e) {
    return new ResponseEntity<>(new Errors()
      .errors(Collections.singletonList(new Error()
        .message(e.getMessage())
        .code(IO_ERROR.getDescription())
        .type(org.folio.dew.domain.dto.ErrorType.ERROR)))
      .totalRecords(1),
      HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ConfigurationException.class)
  public ResponseEntity<Errors> handleConfigurationException(final ConfigurationException e) {
    return new ResponseEntity<>(new Errors()
      .errors(Collections.singletonList(new Error()
        .message(e.getMessage())
        .code(PROCESSING_ERROR.getDescription())
        .type(org.folio.dew.domain.dto.ErrorType.ERROR)))
      .totalRecords(1),
      HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(NonSupportedEntityException.class)
  public ResponseEntity<Errors> handleNonSupportedEntityTypeException(final NonSupportedEntityException e) {
    return new ResponseEntity<>(new Errors()
      .errors(Collections.singletonList(new Error()
        .message(e.getMessage())
        .code(VALIDATION_ERROR.getDescription())
        .type(org.folio.dew.domain.dto.ErrorType.ERROR)))
      .totalRecords(1),
      HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Errors> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
    var parameters = e.getBindingResult().getAllErrors().stream()
      .map(this::processValidationError)
      .collect(Collectors.toList());
    return new ResponseEntity<>(new Errors()
      .errors(Collections.singletonList(new Error()
        .message("Invalid request body")
        .parameters(parameters)
        .code(VALIDATION_ERROR.getDescription())
        .type(org.folio.dew.domain.dto.ErrorType.ERROR)))
      .totalRecords(1),
      HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ContentUpdateValidationException.class)
  public ResponseEntity<Errors> handleContentUpdateValidationException(final ContentUpdateValidationException e) {
    return new ResponseEntity<>(new Errors()
      .errors(Collections.singletonList(new Error()
        .message(e.getMessage())
        .code(VALIDATION_ERROR.getDescription())
        .type(org.folio.dew.domain.dto.ErrorType.ERROR)))
      .totalRecords(1),
      HttpStatus.BAD_REQUEST);
  }

  private Parameter processValidationError(ObjectError error) {
    return new Parameter()
      .key(((FieldError) error).getField())
      .value(error.getDefaultMessage());
  }
}
