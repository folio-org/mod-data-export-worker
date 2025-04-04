package org.folio.dew.utils;

import org.apache.commons.lang3.StringUtils;

public class NonEmpty {

  private String value;

  private NonEmpty(String value) {
    this.value = value;
  }

  public static NonEmpty of(String value) {
      return new NonEmpty(value);
  }

  public String orElse(String value) {
    if (StringUtils.isEmpty(this.value)) {
      return value;
    }
    return this.value;
  }

}
