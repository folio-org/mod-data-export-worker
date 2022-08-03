package org.folio.de.entity;

import lombok.Getter;

@Getter
public enum JobCommandType {
  START("START"), DELETE("DELETE");

  private String value;

  JobCommandType(String value) {
    this.value = value;
  }

  public static JobCommandType fromValue(String value) {
    for (var type : JobCommandType.values()) {
      if (type.getValue().equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
