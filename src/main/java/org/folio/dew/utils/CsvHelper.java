package org.folio.dew.utils;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.experimental.UtilityClass;
import org.folio.dew.repository.LocalFilesStorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.List;

@UtilityClass
public class CsvHelper {
  public static <T> List<T> readRecordsFromFile(LocalFilesStorage localFilesStorage, String fileName, Class<T> clazz, boolean skipHeaders) throws IOException {
    try (var fileReader = new BufferedReader(new InputStreamReader(localFilesStorage.newInputStream(fileName)))) {
      return new CsvToBeanBuilder<T>(fileReader)
        .withType(clazz)
        .withSkipLines(skipHeaders ? 1 : 0)
        .build()
        .parse();
    }
  }

  public static <T> void saveRecordsToLocalFilesStorage(LocalFilesStorage localFilesStorage, List<T> beans, Class<T> clazz, String fileName)
    throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
    var strategy = new RecordColumnMappingStrategy<T>();
    strategy.setType(clazz);
    try (BufferedWriter writer = localFilesStorage.writer(fileName)) {
      new StatefulBeanToCsvBuilder<T>(writer)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build()
        .write(beans);
    }
  }

  public static long countLines(LocalFilesStorage localFilesStorage, String path, boolean skipHeaders) throws IOException {
    try (var lines = localFilesStorage.lines(path)) {
      return skipHeaders ? lines.count() - 1 : lines.count();
    }
  }
}
