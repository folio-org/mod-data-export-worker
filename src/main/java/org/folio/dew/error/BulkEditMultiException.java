package org.folio.dew.error;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom exception class that represents multiple BulkEdit exceptions.
 */
@Getter
public class BulkEditMultiException extends RuntimeException {

  private final List<BulkEditException> exceptions = new ArrayList<>();

  public BulkEditMultiException(List<BulkEditException> exceptions) {
    this.exceptions.addAll(exceptions);
  }
}
