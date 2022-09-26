package org.folio.dew.utils;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.experimental.UtilityClass;
import org.folio.dew.repository.BaseFilesStorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class CsvHelper {
  public static <T, R extends BaseFilesStorage> List<T> readRecordsFromStorage(R storage, String fileName, Class<T> clazz, boolean skipHeaders) throws IOException {
    try (var reader = new BufferedReader(new InputStreamReader(storage.newInputStream(fileName)))) {
      return new CsvToBeanBuilder<T>(reader)
        .withType(clazz)
        .withSkipLines(skipHeaders ? 1 : 0)
        .build()
        .parse();
    }
  }

  public static <T, R extends BaseFilesStorage> List<T> readRecordsFromRemoteFilesStorage(R storage, String fileName, int limit, Class<T> clazz)
    throws IOException {
    try (var reader = new BufferedReader(new InputStreamReader(storage.newInputStream(fileName)))) {
      var linesString = reader.lines().skip(1).limit(limit).collect(Collectors.joining("\n"));
      return new CsvToBeanBuilder<T>(new StringReader(linesString))
        .withType(clazz)
        .build()
        .parse();
    }
  }

  public static <T, R extends BaseFilesStorage> void saveRecordsToStorage(R storage, List<T> beans, Class<T> clazz, String fileName)
    throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
    var strategy = new RecordColumnMappingStrategy<T>();
    strategy.setType(clazz);

    try (BufferedWriter writer = storage.writer(fileName)) {
      new StatefulBeanToCsvBuilder<T>(writer)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build()
        .write(beans);
    }
  }

  public static <R extends BaseFilesStorage>  long countLines(R storage, String path, boolean skipHeaders) throws IOException {
    try (var lines = storage.lines(path)) {
      return skipHeaders ? lines.count() - 1 : lines.count();
    }
  }
}
