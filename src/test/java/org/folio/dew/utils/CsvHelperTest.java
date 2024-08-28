package org.folio.dew.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class CsvHelperTest extends BaseBatchTest {
  private static final String OUT_PATH = "test-dir/out.csv";
  @Autowired
  private LocalFilesStorage localFilesStorage;

  @Test
  void shouldWriteRecordsToCsv() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
    var itemFormats = IntStream.rangeClosed(1, 1100)
      .mapToObj(i -> ItemFormat.builder().id(UUID.randomUUID().toString()).barcode(Integer.toString(i)).build())
      .collect(Collectors.toList());
    CsvHelper.saveRecordsToStorage(localFilesStorage, itemFormats, ItemFormat.class, OUT_PATH);
    // expect header + 1100 records
    assertThat(localFilesStorage.lines(OUT_PATH).count()).isEqualTo(1101);
    // Clean crated files
    localFilesStorage.delete(OUT_PATH);
  }
}
