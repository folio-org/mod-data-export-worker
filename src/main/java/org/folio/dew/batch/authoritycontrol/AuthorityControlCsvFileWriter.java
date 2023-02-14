package org.folio.dew.batch.authoritycontrol;

import org.folio.dew.domain.dto.authoritycontrol.AuthorityUpdateHeadingExportFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
import org.folio.dew.utils.ExportFormatHelper;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.dew.utils.ExportFormatHelper.getHeaderLine;

@Component
@StepScope
public class AuthorityControlCsvFileWriter extends AbstractFileItemWriter<AuthorityUpdateHeadingExportFormat> {
  private final String headersLine;
  private final String tempOutputFilePath;
  private final LocalFilesStorage localFilesStorage;

  public AuthorityControlCsvFileWriter(@Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
                                       LocalFilesStorage localFilesStorage) {
    setResource(tempOutputFilePath);

    this.setExecutionContextName(ClassUtils.getShortName(AuthorityControlCsvFileWriter.class));
    this.headersLine = getHeaderLine(AuthorityUpdateHeadingExportFormat.class, lineSeparator);
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
    writeString(headersLine);
  }

  @Override
  public void write(@NotNull Chunk<? extends AuthorityUpdateHeadingExportFormat> items) throws Exception {
    writeString(doWrite(items));
  }

  @NotNull
  @Override
  protected String doWrite(Chunk<? extends AuthorityUpdateHeadingExportFormat> items) {
    return items.getItems().stream()
      .map(ExportFormatHelper::getItemRow)
      .collect(Collectors.joining(lineSeparator, EMPTY, lineSeparator));
  }

  private void writeString(String str) throws IOException {
    localFilesStorage.append(tempOutputFilePath, str.getBytes(StandardCharsets.UTF_8));
  }

  private void setResource(String tempOutputFilePath) {
    if (isBlank(tempOutputFilePath)) {
      throw new IllegalArgumentException("tempOutputFilePath is blank");
    }
    setResource(new S3CompatibleResource<>(tempOutputFilePath, localFilesStorage));
  }
}
