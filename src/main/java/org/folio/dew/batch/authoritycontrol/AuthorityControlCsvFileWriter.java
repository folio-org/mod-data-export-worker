package org.folio.dew.batch.authoritycontrol;

import org.folio.dew.domain.dto.authoritycontrol.AuthorityControlExportFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
import static org.folio.dew.utils.Constants.COMMA;
import static org.folio.dew.utils.Constants.LINE_BREAK;
import static org.folio.dew.utils.Constants.LINE_BREAK_REPLACEMENT;
import static org.folio.dew.utils.Constants.QUOTE;
import static org.folio.dew.utils.Constants.QUOTE_REPLACEMENT;

@Component
@StepScope
public class AuthorityControlCsvFileWriter extends AbstractFileItemWriter<AuthorityControlExportFormat> {
  private final List<String> headers;
  private final String tempOutputFilePath;
  private final LocalFilesStorage localFilesStorage;

  public AuthorityControlCsvFileWriter(@Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
                                       LocalFilesStorage localFilesStorage) {
    setResource(tempOutputFilePath);

    this.setExecutionContextName(ClassUtils.getShortName(AuthorityControlCsvFileWriter.class));
    this.headers = convertFields(AuthorityControlExportFormat.class.getDeclaredFields());
    this.tempOutputFilePath = tempOutputFilePath;
    this.localFilesStorage = localFilesStorage;
  }

  @Override
  public void afterPropertiesSet() {
    if (append) {
      shouldDeleteIfExists = false;
    }
  }

  @NotNull
  @Override
  protected String doWrite(List<? extends AuthorityControlExportFormat> items) {
    return items.stream()
      .map(item -> getItemRow(item, headers))
      .collect(Collectors.joining(lineSeparator, EMPTY, lineSeparator));
  }

  @Override
  public void write(@NotNull List<? extends AuthorityControlExportFormat> items) throws Exception {
    var headersLine = getHeader(headers) + lineSeparator;
    writeString(headersLine);
    writeString(doWrite(items));
  }

  private void writeString(String str) throws IOException {
    localFilesStorage.append(tempOutputFilePath, str.getBytes(StandardCharsets.UTF_8));
  }

  private String getHeader(List<String> fieldNames) {
    return fieldNames.stream()
      .map(this::headerColumns)
      .flatMap(List::stream)
      .collect(Collectors.joining(","));
  }

  private List<String> headerColumns(String fieldName) {
    return List.of(capitalize(join(splitByCharacterTypeCamelCase(fieldName), SPACE)));
  }

  private String getItemRow(Object item, List<String> exportFieldNames) {
    var itemValues = new ArrayList<String>();
    var bw = new BeanWrapperImpl(item);
    for (var fieldName : exportFieldNames) {
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

  private String quoteValue(String s) {
    if (s.contains(COMMA) || s.contains(LINE_BREAK)) {
      s = QUOTE + s.replace(QUOTE, QUOTE_REPLACEMENT).replace(LINE_BREAK, LINE_BREAK_REPLACEMENT) + QUOTE;
    }
    return s;
  }

  private List<String> convertFields(Field[] fields) {
    return Arrays.stream(fields)
      .map(Field::getName)
      .collect(Collectors.toList());
  }

  private void setResource(String tempOutputFilePath) {
    if (isBlank(tempOutputFilePath)) {
      throw new IllegalArgumentException("tempOutputFilePath is blank");
    }
    setResource(new S3CompatibleResource<>(tempOutputFilePath, localFilesStorage));
  }

  private String getStringValue(String value) {
    return quoteValue(value);
  }
}
