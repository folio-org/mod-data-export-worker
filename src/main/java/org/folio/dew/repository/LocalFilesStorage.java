package org.folio.dew.repository;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.error.FileOperationException;
import org.folio.s3.client.FolioS3Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Local FS to S3-compatible storage adapter.
 */
@Repository
@Log4j2
public class LocalFilesStorage implements S3CompatibleStorage{

  @Autowired
  @Qualifier("localFolioS3Client")
  private FolioS3Client localFolioS3Client;

  @Override
  public String upload(String path, String filename) throws IOException {
    return localFolioS3Client.upload(path, filename);
  }

  @Override
  public void append(String path, byte[] bytes) throws IOException {
    localFolioS3Client.append(path, new ByteArrayInputStream(bytes));
  }

  @Override
  public String write(String path, byte[] bytes) throws IOException {
    return localFolioS3Client.write(path, new ByteArrayInputStream(bytes));
  }

  @Override
  public String write(String path, byte[] bytes, Map<String, String> headers) throws IOException {
    return write(path, bytes);
  }

  @Override
  public boolean exists(String path) {
    return CollectionUtils.isNotEmpty(localFolioS3Client.list(path));
  }

  @Override
  public InputStream newInputStream(String path) throws IOException {
    return localFolioS3Client.read(path);
  }

  @Override
  public byte[] readAllBytes(String path) throws IOException {
    return localFolioS3Client.read(path).readAllBytes();
  }

  public String writeFile(String destPath, Path inputPath) throws IOException {
    return upload(inputPath.toString(), destPath);
  }

  public void delete(String path) {
    localFolioS3Client.remove(path);
  }

  public boolean notExists(String path) {
    return !exists(path);
  }

  public Stream<String> walk(String path) {
    return localFolioS3Client.list(path).stream();
  }

  public BufferedWriter writer(String path) {
    return new BufferedWriter(new OutputStreamWriter(newOutputStream(path)));
  }

  public OutputStream newOutputStream(String path) {


    return new OutputStream() {

      byte[] buffer = new byte[0];

      @Override
      public void write(int b) {
        buffer = ArrayUtils.add(buffer, (byte) b);
      }

      @Override
      public void flush() {
//        throw new NotImplementedException("Method isn't implemented yet");
      }

      @Override
      public void close() {
        try {
          LocalFilesStorage.this.write(path, buffer);
        } catch (IOException e) {
          throw new FileOperationException("Error closing stream and writes bytes to path: " + path, e);
        } finally {
          buffer = new byte[0];
        }
      }
    };
  }

  public Stream<String> lines(String path) throws IOException {
    return new BufferedReader(new InputStreamReader(newInputStream(path))).lines();
  }

  public List<String> readAllLines(String path) throws IOException {
    try (var lines = lines(path)) {
      return lines.collect(Collectors.toList());
    } catch(Exception e) {
      throw new IOException("Error reading file: " + path, e);
    }
  }

  public List<String> linesNumber(String path, int num) throws IOException {
    try (var reader = new BufferedReader(new InputStreamReader(newInputStream(path)))) {
      List<String> list = new ArrayList<>();
      for (int i = 0; i < num; i++) {
        var line = reader.readLine();
        if (StringUtils.isNotBlank(line)) {
          list.add(line);
        }
      }
      return list;
    }
  }
}
