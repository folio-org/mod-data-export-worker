package org.folio.dew.batch.authoritycontrol;

import lombok.SneakyThrows;
import org.folio.dew.domain.dto.authoritycontrol.AuthorityControlExportFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthorityControlCsvFileWriterTest {
  @Mock
  private LocalFilesStorage localFilesStorage;
  private AuthorityControlCsvFileWriter authorityControlCsvFileWriter;

  private final String TEMP_FILE = "test.csv";

  @SneakyThrows
  @BeforeEach
  void setUp() {
    authorityControlCsvFileWriter = new AuthorityControlCsvFileWriter(TEMP_FILE, localFilesStorage);
    lenient().doNothing().when(localFilesStorage).append(anyString(), any());
  }

  @Test
  @SneakyThrows
  void testWriteHeadersBeforeStepMethod(){
    //When
    authorityControlCsvFileWriter.beforeStep();

    //Then
    verify(localFilesStorage).append(eq(TEMP_FILE), any());
  }

  @Test
  @SneakyThrows
  void testWriteItemsMethod(){
    //Given
    List<AuthorityControlExportFormat> items = new ArrayList<>();
    AuthorityControlExportFormat exportFormat = new AuthorityControlExportFormat();
    exportFormat.setUpdater("Test User");
    items.add(exportFormat);

    //When
    authorityControlCsvFileWriter.write(items);

    //Then
    verify(localFilesStorage).append(eq(TEMP_FILE), any());
  }
}
