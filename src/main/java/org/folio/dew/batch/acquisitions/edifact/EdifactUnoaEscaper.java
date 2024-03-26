package org.folio.dew.batch.acquisitions.edifact;

import org.springframework.stereotype.Component;

@Component
public class EdifactUnoaEscaper {

  private static final String ESCAPER = "?";
  private static final String SPECIAL_CHARACTERS = "?+.'";

  public String escape(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (SPECIAL_CHARACTERS.indexOf(s.charAt(i)) != -1) {
        s = s.substring(0, i) + ESCAPER + s.substring(i++);
      }
    }
    return s;
  }

}
