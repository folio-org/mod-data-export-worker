package org.folio.dew.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@UtilityClass
@Log4j2
public class Utils {

  public static String encode(CharSequence s) {
    if (s == null) {
      return "\"\"";
    }
    var appendable = new StringBuilder(s.length() + 2);
    appendable.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\', '*', '?', '^', '"' -> appendable.append('\\').append(c);
        default -> appendable.append(c);
      }
    }
    appendable.append('"');
    return appendable.toString();
  }
}
