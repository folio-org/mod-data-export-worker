package org.folio.dew.utils;

import lombok.experimental.UtilityClass;

import java.io.File;

import static org.folio.dew.utils.Constants.CHARACTERS_SHOULD_BE_REPLACED_IN_PATH;

@UtilityClass
public class SystemHelper {
  public static String getTempDirWithSeparatorSuffix() {
    var dir = System.getProperty("java.io.tmpdir");
    return dir.endsWith(File.separator) ? dir : dir + File.separator;
  }

  public static String validatePath(String path) {
    return path.replaceAll(CHARACTERS_SHOULD_BE_REPLACED_IN_PATH, "_");
  }
}
