package org.folio.dew.utils;

import org.folio.dew.domain.dto.annotation.ExportFormat;
import org.folio.dew.domain.dto.annotation.ExportHeader;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

public class ExportFormatHelper {

  public static String getHeaderLine(Class<?> clazz, String lineSeparator) {
    var headers = getExportFormatHeaders(clazz);
    return getHeaderLine(headers, lineSeparator);
  }

  public static String getHeaderLine(List<String> headers, String lineSeparator) {
    return String.join(",", headers) + lineSeparator;
  }

  public static List<String> getExportFormatHeaders(Class<?> clazz) {
    if (!clazz.isAnnotationPresent(ExportFormat.class)) {
      throw new IllegalArgumentException("Class not annotated as export format");
    }
    var exportFormat = clazz.getAnnotation(ExportFormat.class);

    return Arrays.stream(clazz.getDeclaredFields())
      .map(field -> getFieldColumnName(exportFormat, field))
      .collect(Collectors.toList());
  }

  private static String getFieldColumnName(ExportFormat exportFormat, Field field) {
    if (field.isAnnotationPresent(ExportHeader.class)) {
      return field.getAnnotation(ExportHeader.class).value();
    }

    var camelCaseString = join(splitByCharacterTypeCamelCase(field.getName()), SPACE);
    if (exportFormat.capitalizeHeaders()) {
      return capitalize(camelCaseString);
    }
    return camelCaseString;
  }
}
