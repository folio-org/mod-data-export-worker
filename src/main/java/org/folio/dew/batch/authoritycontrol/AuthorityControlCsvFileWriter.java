package org.folio.dew.batch.authoritycontrol;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.dew.utils.ExportFormatHelper.getHeaderLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.folio.dew.domain.dto.authoritycontrol.exportformat.AuthorityControlExportFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
import org.folio.dew.utils.ExportFormatHelper;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
@StepScope
public class AuthorityControlCsvFileWriter extends AbstractFileItemWriter<AuthorityControlExportFormat> {
  private final String headersLine;
  private final String tempOutputFilePath;
  private final LocalFilesStorage localFilesStorage;

  public AuthorityControlCsvFileWriter(Class<? extends AuthorityControlExportFormat> exportFormatClass,
                                       @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
                                       LocalFilesStorage localFilesStorage) {
    setResource(tempOutputFilePath);

    this.setExecutionContextName(ClassUtils.getShortName(exportFormatClass));
    this.headersLine = getHeaderLine(exportFormatClass, lineSeparator);
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

  @AfterStep
  public void afterStep() throws IOException {
    var lines = localFilesStorage.linesNumber(tempOutputFilePath, 2);
    if (lines.size() == 1) {
      writeString("No records found");
    }
  }

  @Override
  public void write(@NotNull Chunk<? extends AuthorityControlExportFormat> items) throws Exception {
    writeString(doWrite(items));
  }

  @NotNull
  @Override
  protected String doWrite(Chunk<? extends AuthorityControlExportFormat> chunk) {
    return chunk.getItems().stream()
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
