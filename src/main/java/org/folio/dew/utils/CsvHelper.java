package org.folio.dew.utils;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.experimental.UtilityClass;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@UtilityClass
public class CsvHelper {
  public static <T> List<T> readRecordsFromFile(String fileName, Class<T> clazz, boolean skipHeaders) throws FileNotFoundException {
    return new CsvToBeanBuilder<T>(new FileReader(fileName))
      .withType(clazz)
      .withSkipLines(skipHeaders ? 1 : 0)
      .build()
      .parse();
  }

  public static <T> void saveRecordsToCsv(List<T> beans, Class<T> clazz, String fileName)
    throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
    var strategy = new RecordColumnMappingStrategy<T>();
    strategy.setType(clazz);
    try (BufferedWriter writer = Files.newBufferedWriter(Path.of(fileName))) {
      new StatefulBeanToCsvBuilder<T>(writer)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build()
        .write(beans);
    }
  }

  public static long countLines(Path path, boolean skipHeaders) throws IOException {
    try (var lines = Files.lines(path)) {
      return skipHeaders ? lines.count() - 1 : lines.count();
    }
  }
}
