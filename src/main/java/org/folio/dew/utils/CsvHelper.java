package org.folio.dew.utils;

import static org.folio.dew.utils.Constants.LINE_BREAK;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.repository.BaseFilesStorage;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
@Log4j2
public class CsvHelper {
  private static final int BATCH_SIZE = 1000;

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

  public static <T> void saveRecordsToStorage(LocalFilesStorage storage, List<T> beans, Class<T> clazz, String fileName)
    throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
    var strategy = new RecordColumnMappingStrategy<T>();
    strategy.setType(clazz);

    if (storage.exists(fileName)) {
      storage.delete(fileName);
    }

    if (beans.size() > BATCH_SIZE) {
      for (int batchNumber = 0; batchNumber <= beans.size() / BATCH_SIZE; batchNumber++) {
        log.info("Writing batch #{}", batchNumber);
        var batch = beans.stream()
          .skip((long) batchNumber * BATCH_SIZE)
          .limit(BATCH_SIZE)
          .collect(Collectors.toList());
        try (var stringWriter = new StringWriter()) {
          new StatefulBeanToCsvBuilder<T>(stringWriter)
            .withApplyQuotesToAll(false)
            .withMappingStrategy(strategy)
            .build()
            .write(batch);
          var csvString = stringWriter.toString();
          storage.append(fileName, batchNumber == 0 ? csvString.getBytes() : csvString.substring(csvString.indexOf(LINE_BREAK) + 1).getBytes());
        }
      }
    } else {
      try (var writer = storage.writer(fileName)) {
        new StatefulBeanToCsvBuilder<T>(writer)
          .withApplyQuotesToAll(false)
          .withMappingStrategy(strategy)
          .build()
          .write(beans);
      }
    }
  }

  public static long countLines(LocalFilesStorage storage, String path) throws IOException {
    try (var lines = storage.lines(path)) {
      return lines.count();
    }
  }
}
