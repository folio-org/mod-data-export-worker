package org.folio.dew.service;

import lombok.Getter;

@Getter
public class ErrorMessage {

  private String value;

  public void setValue(String value) {
    if (this.value == null) this.value = value;
  }
}
