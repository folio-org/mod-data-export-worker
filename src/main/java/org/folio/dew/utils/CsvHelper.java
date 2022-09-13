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
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.io.OutputStreamWriter;
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

  public static <T> List<T> readRecordsFromMinio(RemoteFilesStorage remoteFilesStorage, String fileName, Class<T> clazz, boolean skipHeaders)
    throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
    InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    try (var reader = new CSVReader(new InputStreamReader(remoteFilesStorage.getObject(fileName)))) {
      reader.skip(skipHeaders ? 1 : 0);
      return new CsvToBeanBuilder<T>(reader)
        .withType(clazz)
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

  public static <T> void saveRecordsToMinio(RemoteFilesStorage remoteFilesStorage, List<T> beans, Class<T> clazz, String fileName)
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
      remoteFilesStorage.putObject(stream.toByteArray(), fileName);
    }
  }
}
