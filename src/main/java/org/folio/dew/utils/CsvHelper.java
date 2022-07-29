package org.folio.dew.utils;

import com.opencsv.CSVReader;
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
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

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

  public static <T> List<T> readRecordsFromMinio(RemoteFilesStorage remoteFilesStorage, String fileName, int limit, Class<T> clazz)
    throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
    InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    try (var reader = new BufferedReader(new InputStreamReader(remoteFilesStorage.getObject(fileName)))) {
      var linesString = reader.lines().skip(1).limit(limit).collect(Collectors.joining("\n"));
      return new CsvToBeanBuilder<T>(new StringReader(linesString))
        .withType(clazz)
        .build()
        .parse();
    }
  }

  public static <T> void saveRecordsToLocalFilesStorage(LocalFilesStorage localFilesStorage, List<T> beans, Class<T> clazz, String fileName)
    throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
    var strategy = new RecordColumnMappingStrategy<T>();
    strategy.setType(clazz);
    if (!beans.isEmpty()) {
      try (BufferedWriter writer = localFilesStorage.writer(fileName)) {
        new StatefulBeanToCsvBuilder<T>(writer)
          .withApplyQuotesToAll(false)
          .withMappingStrategy(strategy)
          .build()
          .write(beans);
      }
    } else {
      localFilesStorage.write(fileName, new byte[0]);
    }
  }

  public static <T> List<T> readRecordsFromMinio(RemoteFilesStorage repository, String fileName, Class<T> clazz, boolean skipHeaders)
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

  public static <T> void saveRecordsToMinio(RemoteFilesStorage remoteFilesStorage, List<T> beans, Class<T> clazz, String fileName)
    throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
    if (!beans.isEmpty()) {
      var strategy = new RecordColumnMappingStrategy<T>();
      strategy.setType(clazz);
      try (BufferedWriter writer = remoteFilesStorage.writer(fileName)) {
        new StatefulBeanToCsvBuilder<T>(writer)
          .withApplyQuotesToAll(false)
          .withMappingStrategy(strategy)
          .build()
          .write(beans);
      }

    } else {
      remoteFilesStorage.write(fileName, new byte[0]);
    }

  }

  public static long countLines(LocalFilesStorage localFilesStorage, String path, boolean skipHeaders) throws IOException {
    try (var lines = localFilesStorage.lines(path)) {
      return skipHeaders ? lines.count() - 1 : lines.count();
    }
  }
}
