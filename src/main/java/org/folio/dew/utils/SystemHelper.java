package org.folio.dew.utils;

import lombok.experimental.UtilityClass;

import java.io.File;

@UtilityClass
public class SystemHelper {
  public static String getTempDirWithSeparatorSuffix() {
    var dir = System.getProperty("java.io.tmpdir");
    return dir.endsWith(File.separator) ? dir : dir + File.separator;
  }
}
