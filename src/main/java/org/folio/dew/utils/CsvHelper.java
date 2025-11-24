package org.folio.dew.utils;

import static org.folio.dew.utils.Constants.LINE_BREAK;
import static org.folio.dew.utils.Constants.LINE_BREAK_REPLACEMENT;
import static org.folio.dew.utils.Constants.UTF8_BOM;

import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.repository.BaseFilesStorage;

@UtilityClass
@Log4j2
public class CsvHelper {

  public static <T, R extends BaseFilesStorage> List<T> readRecordsFromStorage(R storage, String fileName, Class<T> clazz, boolean skipHeaders) throws IOException {
    try (var reader = new BufferedReader(new InputStreamReader(storage.newInputStream(fileName)))) {
      return new CsvToBeanBuilder<T>(reader)
        .withType(clazz)
        .withSkipLines(skipHeaders ? 1 : 0)
        .build()
        .parse();
    }
  }

  public static <R extends BaseFilesStorage> long countLines(R storage, String path) throws IOException {
    try (var lines = storage.lines(path)) {
      return lines.count();
    }
  }


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
