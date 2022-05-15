package org.folio.dew.utils;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.experimental.UtilityClass;

import java.io.BufferedWriter;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@UtilityClass
public class CsvHelper {
  public static <T> List<T> readRecordsFromFile(String fileName, Class<T> clazz, boolean skipHeaders) throws IOException {
    try (Reader fileReader = new FileReader(fileName)) {
      return new CsvToBeanBuilder<T>(fileReader)
        .withType(clazz)
        .withSkipLines(skipHeaders ? 1 : 0)
        .build()
        .parse();
    }
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
}
