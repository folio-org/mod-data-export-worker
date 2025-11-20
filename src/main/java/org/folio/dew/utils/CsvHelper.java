package org.folio.dew.utils;

import static org.folio.dew.utils.Constants.LINE_BREAK;
import static org.folio.dew.utils.Constants.LINE_BREAK_REPLACEMENT;
import static org.folio.dew.utils.Constants.UTF8_BOM;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;


@UtilityClass
@Log4j2
public class CsvHelper {

  /**
   * Escapes delimiter in the value with escape character. If value contains delimiter, line break or escape character,
   * it will be escaped - as instructed in <a href="https://www.ietf.org/rfc/rfc4180.txt">RFC 4180</a>
   *
   * @param value      value to process
   * @param delimiter  delimiter to escape
   * @param escape     escape character
   * @return escaped value
   */
  public static String escapeDelimiter(String value, String delimiter, String escape) {
    return StringUtils.isNotBlank(value) && value.contains(delimiter)
      ? escape + value.replace(escape, escape + escape).replace(LINE_BREAK, LINE_BREAK_REPLACEMENT) + escape
      : value;
  }

  public static String clearBomSymbol(String obj) {
    if (obj.startsWith(UTF8_BOM)) {
      obj = obj.substring(1);
    }
    return obj;
  }
}
