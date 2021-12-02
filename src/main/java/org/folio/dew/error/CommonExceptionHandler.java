package org.folio.dew.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.service.SaveErrorService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Log4j2
@RequiredArgsConstructor
@ControllerAdvice
public class CommonExceptionHandler {

  private final SaveErrorService saveErrorService;

  @ExceptionHandler(Exception.class)
  public void defaultErrorHandler(final Exception exception) {
    log.error("Exception other than BulkEditException occurred: {}", exception);
  }
}
