package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class DesiredPermissionsUtil {
  private static final String OPEN_SIGN = "[";
  private static final String CLOSE_SIGN = "]";
  private static final String COMMA = ",";
  private static final String QUOTE = "\"";

  private DesiredPermissionsUtil(){}

  public static List<String> convertPermissionsToList(String permissions) {
    if (StringUtils.isEmpty(permissions)) {
      return List.of();
    }
    permissions = StringUtils.remove(permissions, OPEN_SIGN);
    permissions = StringUtils.remove(permissions, CLOSE_SIGN);
    permissions = StringUtils.remove(permissions, QUOTE);
    var splitted = permissions.split(COMMA);
    return List.of(splitted);
  }
}
