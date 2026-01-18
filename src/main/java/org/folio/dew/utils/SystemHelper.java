package org.folio.dew.utils;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.folio.dew.utils.Constants.CHARACTERS_SHOULD_BE_REPLACED_IN_PATH;

@UtilityClass
public class SystemHelper {

  private static final Pattern INVALID_PATH_CHARS =
      Pattern.compile(CHARACTERS_SHOULD_BE_REPLACED_IN_PATH);

  public static String getTempDirWithSeparatorSuffix() {
    var dir = System.getProperty("java.io.tmpdir");
    return dir.endsWith(File.separator) ? dir : dir + File.separator;
  }

  public static String validatePath(String path) {
    Objects.requireNonNull(path, "Path must not be null");
    return INVALID_PATH_CHARS.matcher(path).replaceAll("_");
  }
}