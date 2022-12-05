package org.folio.dew.service.mapper;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class MapperHelper {
  public static String restoreStringValue(String s) {
    return isEmpty(s) || "null".equalsIgnoreCase(s) ? null : s;
  }

}
