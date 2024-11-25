package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import java.nio.charset.StandardCharsets;

public class Utils {

  private Utils() {}

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

  public static String getUtf8Bom() {
    return new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8);
  }
}
