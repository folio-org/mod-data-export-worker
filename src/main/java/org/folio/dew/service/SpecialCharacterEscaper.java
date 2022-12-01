package org.folio.dew.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;
import static org.folio.dew.utils.Constants.KEY_VALUE_DELIMITER;

@Component
public class SpecialCharacterEscaper {

  private static final String[] specialCharacters = {ITEM_DELIMITER , ARRAY_DELIMITER, KEY_VALUE_DELIMITER};
  private static final String[] escapedValues = {"%7C", "%3B", "%3A"};

  public String escape(String initial) {
    if (StringUtils.isEmpty(initial)) return initial;
    for (int i = 0; i < specialCharacters.length; i++) {
      initial = initial.replace(specialCharacters[i], escapedValues[i]);
    }
    return initial;
  }

  public List<String> escape(List<String> initial) {
    if (initial == null) return null;
    return initial.stream().map(this::escape).collect(Collectors.toList());
  }


  public String restore(String escaped) {
    if (StringUtils.isEmpty(escaped)) return escaped;
    for (int i = 0; i < escapedValues.length; i++) {
      escaped = escaped.replace(escapedValues[i], specialCharacters[i]);
    }
    return escaped;
  }

  public List<String> restore(List<String> escaped) {
    if (escaped == null) return null;
    return escaped.stream().map(this::restore).collect(Collectors.toList());
  }
}
