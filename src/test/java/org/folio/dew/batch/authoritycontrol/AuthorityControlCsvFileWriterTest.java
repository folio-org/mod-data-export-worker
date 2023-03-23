package org.folio.dew.batch.authoritycontrol;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.folio.dew.domain.dto.authoritycontrol.exportformat.AuthUpdateHeadingExportFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

@ExtendWith(MockitoExtension.class)
class AuthorityControlCsvFileWriterTest {
  private final String TEMP_FILE = "test.csv";
  @Mock
  private LocalFilesStorage localFilesStorage;
  private AuthorityControlCsvFileWriter authorityControlCsvFileWriter;

  @SneakyThrows
  @BeforeEach
  void setUp() {
    authorityControlCsvFileWriter = new AuthorityControlCsvFileWriter(AuthUpdateHeadingExportFormat.class, TEMP_FILE, localFilesStorage);
    lenient().doNothing().when(localFilesStorage).append(anyString(), any());
  }

  @Test
  @SneakyThrows
  void testWriteHeadersBeforeStepMethod() {
    //When
    authorityControlCsvFileWriter.beforeStep();

    //Then
    verify(localFilesStorage).append(eq(TEMP_FILE), any());
  }

  @Test
  @SneakyThrows
  void testWriteHeadersAfterStepMethod_whenStatsExist() {
    when(localFilesStorage.linesNumber(TEMP_FILE, 2)).thenReturn(List.of("headers", "line2"));

    //When
    authorityControlCsvFileWriter.afterStep();

    //Then
    verify(localFilesStorage, never()).append(eq(TEMP_FILE), any());
  }

  @Test
  @SneakyThrows
  void testWriteHeadersAfterStepMethod_whenStatsNotExist() {
    when(localFilesStorage.linesNumber(TEMP_FILE, 2)).thenReturn(List.of("headers"));

    //When
    authorityControlCsvFileWriter.afterStep();

    //Then
    verify(localFilesStorage).append(TEMP_FILE, "No records found".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  @SneakyThrows
  void testWriteItemsMethod() {
    //Given
    List<AuthUpdateHeadingExportFormat> items = new ArrayList<>();
    AuthUpdateHeadingExportFormat exportFormat = new AuthUpdateHeadingExportFormat();
    exportFormat.setUpdater("Test User");
    items.add(exportFormat);

    //When
    authorityControlCsvFileWriter.write(new Chunk<>(items));

    //Then
    verify(localFilesStorage).append(eq(TEMP_FILE), any());
  }
}
