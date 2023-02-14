package org.folio.dew.batch.authoritycontrol;

import org.folio.dew.domain.dto.authoritycontrol.AuthorityUpdateHeadingExportFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.dew.utils.Constants.COMMA;
import static org.folio.dew.utils.Constants.LINE_BREAK;
import static org.folio.dew.utils.Constants.LINE_BREAK_REPLACEMENT;
import static org.folio.dew.utils.Constants.QUOTE;
import static org.folio.dew.utils.Constants.QUOTE_REPLACEMENT;
import static org.folio.dew.utils.ExportFormatHelper.getExportFormatHeaders;
import static org.folio.dew.utils.ExportFormatHelper.getHeaderLine;

@Component
@StepScope
public class AuthorityControlCsvFileWriter extends AbstractFileItemWriter<AuthorityUpdateHeadingExportFormat> {
  private final List<String> headers;
  private final String tempOutputFilePath;
  private final LocalFilesStorage localFilesStorage;

  public AuthorityControlCsvFileWriter(@Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
                                       LocalFilesStorage localFilesStorage) {
    setResource(tempOutputFilePath);

    this.setExecutionContextName(ClassUtils.getShortName(AuthorityControlCsvFileWriter.class));
    this.headers = getExportFormatHeaders(AuthorityUpdateHeadingExportFormat.class);
    this.tempOutputFilePath = tempOutputFilePath;
    this.localFilesStorage = localFilesStorage;
  }

  @Override
  public void afterPropertiesSet() {
    if (append) {
      shouldDeleteIfExists = false;
    }
  }

  @BeforeStep
  public void beforeStep() throws IOException {
    var headersLine = getHeaderLine(headers, lineSeparator) ;
    writeString(headersLine);
  }

  @Override
  public void write(@NotNull List<? extends AuthorityUpdateHeadingExportFormat> items) throws Exception {
    writeString(doWrite(items));
  }

  @NotNull
  @Override
  protected String doWrite(List<? extends AuthorityUpdateHeadingExportFormat> items) {
    return items.stream()
      .map(item -> getItemRow(item, headers))
      .collect(Collectors.joining(lineSeparator, EMPTY, lineSeparator));
  }

  private void writeString(String str) throws IOException {
    localFilesStorage.append(tempOutputFilePath, str.getBytes(StandardCharsets.UTF_8));
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
