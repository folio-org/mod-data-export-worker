package org.folio.dew.utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import lombok.experimental.UtilityClass;
import org.folio.dew.repository.MinIOObjectStorageRepository;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@UtilityClass
public class CsvHelper {
  public static <T> List<T> readRecordsFromFile(String fileName, Class<T> clazz, boolean skipHeaders) throws IOException {
    try (var fileReader = new FileReader(fileName)) {
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

  public static long countLines(Path path, boolean skipHeaders) throws IOException {
    try (var lines = Files.lines(path)) {
      return skipHeaders ? lines.count() - 1 : lines.count();
    }
  }

  public static <T> List<T> readRecordsFromMinio(MinIOObjectStorageRepository repository, String fileName, Class<T> clazz, boolean skipHeaders)
    throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
    InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    try (var reader = new CSVReader(new InputStreamReader(repository.getObject(fileName)))) {
      reader.skip(skipHeaders ? 1 : 0);
      return new CsvToBeanBuilder<T>(reader)
        .withType(clazz)
        .build()
        .parse();
    }
  }

  public static <T> void saveRecordsToMinio(MinIOObjectStorageRepository repository, List<T> beans, Class<T> clazz, String fileName)
    throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, ServerException, InsufficientDataException,
    ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
    InternalException {
    var strategy = new RecordColumnMappingStrategy<T>();
    strategy.setType(clazz);
    try (var stream = new ByteArrayOutputStream();
      var streamWriter = new OutputStreamWriter(stream);
      var writer = new CSVWriter(streamWriter)) {
      new StatefulBeanToCsvBuilder<T>(writer)
        .withApplyQuotesToAll(false)
        .withMappingStrategy(strategy)
        .build()
        .write(beans);
      streamWriter.flush();
      repository.putObject(stream.toByteArray(), fileName);
    }
  }
}
