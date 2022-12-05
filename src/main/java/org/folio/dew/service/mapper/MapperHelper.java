package org.folio.dew.service.mapper;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MapperHelper {
  public static String restoreStringValue(String s) {
    return isEmpty(s) || "null".equalsIgnoreCase(s) ? null : s;
  }

}
