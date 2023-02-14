package org.folio.dew.utils;

import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.annotation.ExportFormat;
import org.folio.dew.domain.dto.annotation.ExportHeader;
import org.springframework.beans.BeanWrapperImpl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
import static org.folio.dew.utils.Constants.COMMA;
import static org.folio.dew.utils.Constants.LINE_BREAK;
import static org.folio.dew.utils.Constants.LINE_BREAK_REPLACEMENT;
import static org.folio.dew.utils.Constants.QUOTE;
import static org.folio.dew.utils.Constants.QUOTE_REPLACEMENT;

@UtilityClass
public class ExportFormatHelper {

  private static final String NOT_EXPORT_FORMAT = "Class %s not annotated as export format";
  public static String getHeaderLine(Class<?> clazz, String lineSeparator) {
    var headers = getExportFormatHeaders(clazz);
    return getHeaderLine(headers, lineSeparator);
  }

  public static String getHeaderLine(List<String> headers, String lineSeparator) {
    return String.join(",", headers) + lineSeparator;
  }

  public static String getItemRow(Object item) {
    var objectClass = item.getClass();
    verifyAnnotationPresence(objectClass);

    var clazzFields = getClassFields(objectClass);
    var itemValues = new ArrayList<String>();
    var bw = new BeanWrapperImpl(item);
    for (var fieldName : clazzFields) {
      var value = bw.getPropertyValue(fieldName);
      if (value instanceof String) {
        var s = getStringValue((String) value);
        itemValues.add(s);
      } else {
        itemValues.add(EMPTY);
      }
    }
    return String.join(",", itemValues);
  }

  private static List<String> getExportFormatHeaders(Class<?> clazz) {
    verifyAnnotationPresence(clazz);
    var exportFormat = clazz.getAnnotation(ExportFormat.class);
    return Arrays.stream(clazz.getDeclaredFields())
      .map(field -> getFieldColumnName(exportFormat, field))
      .collect(Collectors.toList());
  }

  private static String getFieldColumnName(ExportFormat exportFormat, Field field) {
    if (field.isAnnotationPresent(ExportHeader.class)) {
      return field.getAnnotation(ExportHeader.class).value();
    }

    var splitHeader = splitByCharacterTypeCamelCase(field.getName());
    if (exportFormat.sentenceCaseHeaders()) {
      return capitalize(Arrays.stream(splitHeader)
        .map(ExportFormatHelper::decapitalize)
        .collect(Collectors.joining(SPACE)));
    }

    return capitalize(join(splitHeader, SPACE));
  }

  private static void verifyAnnotationPresence(Class<?> clazz) {
    if (!clazz.isAnnotationPresent(ExportFormat.class)) {
      throw new IllegalArgumentException(format(NOT_EXPORT_FORMAT, clazz.getName()));
    }
  }

  private static String decapitalize(String string) {
    if (string == null || string.length() == 0) {
      return string;
    }

    char[] c = string.toCharArray();
    c[0] = Character.toLowerCase(c[0]);

    return new String(c);
  }

  private static String quoteValue(String s) {
    if (s.contains(COMMA) || s.contains(LINE_BREAK)) {
      s = QUOTE + s.replace(QUOTE, QUOTE_REPLACEMENT).replace(LINE_BREAK, LINE_BREAK_REPLACEMENT) + QUOTE;
    }
    return s;
  }

  private static String getStringValue(String value) {
    return quoteValue(value);
  }

  private static List<String> getClassFields(Class<?> clazz) {
    return Arrays.stream(clazz.getDeclaredFields())
      .map(Field::getName)
      .collect(Collectors.toUnmodifiableList());
  }
}
