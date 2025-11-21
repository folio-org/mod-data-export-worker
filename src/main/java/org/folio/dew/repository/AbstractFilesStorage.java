package org.folio.dew.repository;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.config.properties.MinioClientProperties;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.PutObjectAdditionalOptions;
import org.folio.s3.client.RemoteStorageWriter;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.exception.S3ClientException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.minio.http.Method;
import lombok.extern.log4j.Log4j2;

import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.TMP_PATH_PREFIX;

/** Custom extension of FolioS3Client with additional utility methods */
@Log4j2
public abstract class AbstractFilesStorage implements FolioS3Client {

  private final FolioS3Client client;

  private final MinioClientProperties properties;

  protected AbstractFilesStorage(MinioClientProperties properties) {
    this.client = S3ClientFactory.getS3Client(properties.toS3ClientProperties());
    this.properties = properties;

    this.createBucketIfNotExists();
  }

  @Override
  public String upload(String path, String filename) {
    return client.upload(sanitizePath(path), filename);
  }

  @Override
  public String append(String path, InputStream newData) {
      try {
          if (!exists(path)) {
              return write(path, newData);
          }
          InputStream existingData = read(path);
          try (SequenceInputStream combinedStream = new SequenceInputStream(existingData, newData)) {
              return write(path, combinedStream);
          }
      } catch (IOException e) {
          throw log.throwing(new S3ClientException("Error appending data to file: " + path, e));
      }
  }

  public void append(String path, byte[] bytes) {
    append(path, new ByteArrayInputStream(bytes));
}

  @Override
  public String write(String path, InputStream is) {
    return client.write(sanitizePath(path), is);
  }

  public String write(String path, byte[] bytes) {
    return write(path, new ByteArrayInputStream(bytes));
  }

  @Override
  public String write(String path, InputStream is, long size) {
    return client.write(sanitizePath(path), is, size);
  }

  @Override
  public String write(String path, InputStream is, long size, PutObjectAdditionalOptions extraOptions) {
    return client.write(sanitizePath(path), is, size, extraOptions);
  }

  @Override
  public String compose(String destination, List<String> sourceKeys) {
    return client.compose(sanitizePath(destination), sourceKeys.stream()
      .map(this::sanitizePath)
      .toList());
  }

  @Override
  public String compose(String destination, List<String> sourceKeys, PutObjectAdditionalOptions extraOptions) {
    return client.compose(sanitizePath(destination), sourceKeys.stream()
      .map(this::sanitizePath)
      .toList(), extraOptions);
  }

  @Override
  public String remove(String path) {
    return client.remove(sanitizePath(path));
  }

  @Override
  public List<String> remove(String... paths) {
    return client.remove(Arrays.stream(paths)
      .map(this::sanitizePath)
      .toArray(String[]::new));
  }

  @Override
  public InputStream read(String path) {
    return client.read(sanitizePath(path));
  }

  @Override
  public List<String> list(String path) {
    return client.list(sanitizePath(path));
  }

  @Override
  public List<String> list(String path, int maxKeys, String startAfter) {
    return client.list(sanitizePath(path), maxKeys, startAfter);
  }

  @Override
  public List<String> listRecursive(String path) {
    return client.listRecursive(sanitizePath(path));
  }

  @Override
  public long getSize(String path) {
    return client.getSize(sanitizePath(path));
  }

  @Override
  public RemoteStorageWriter getRemoteStorageWriter(String path, int size) {
    return client.getRemoteStorageWriter(sanitizePath(path), size);
  }

  public RemoteStorageWriter writer(String path) {
    return getRemoteStorageWriter(path, 1024);
  }

  @Override
  public String getPresignedUrl(String path) {
    return getPresignedUrl(path, Method.GET);
  }

  @Override
  public String getPresignedUrl(String path, Method method) {
    return getPresignedUrl(path, method, properties.getUrlExpirationTimeInSeconds(), TimeUnit.SECONDS);
  }

  @Override
  public String getPresignedUrl(String path, Method method, int expiryTime, TimeUnit expiryUnit) {
    return client.getPresignedUrl(sanitizePath(path), method, expiryTime, expiryUnit);
  }

  @Override
  public void createBucketIfNotExists() {
    client.createBucketIfNotExists();
  }

  @Override
  public String initiateMultipartUpload(String path) {
    return client.initiateMultipartUpload(sanitizePath(path));
  }

  @Override
  public String getPresignedMultipartUploadUrl(String path, String uploadId, int partNumber) {
    return client.getPresignedMultipartUploadUrl(sanitizePath(path), uploadId, partNumber);
  }

  @Override
  public String uploadMultipartPart(String path, String uploadId, int partNumber, String filename) {
    return client.uploadMultipartPart(sanitizePath(path), uploadId, partNumber, filename);
  }

  @Override
  public void abortMultipartUpload(String path, String uploadId) {
    client.abortMultipartUpload(sanitizePath(path), uploadId);
  }

  @Override
  public void completeMultipartUpload(String path, String uploadId, List<String> partETags) {
    client.completeMultipartUpload(sanitizePath(path), uploadId, partETags);
  }

  /**
   * Verifies if file exists on storage
   *
   * @param path - the path to the file on S3-compatible storage
   * @return true if file exists, otherwise - false
   */
  public boolean exists(String path) {
    return !list(path, 1, null).isEmpty();
  }

  /**
   * Verifies if file doesn't exist on S3-compatible storage
   *
   * @param path - the path to the file
   * @return true if file doesn't exist, otherwise - false
   */
  public boolean notExists(String path) {
    return !exists(path);
  }

  /**
   * Reads all the bytes from a file
   *
   * @param path - the path to the file on S3-compatible storage
   * @return a byte array containing the bytes read from the file
   * @throws IOException - if an I/O error occurs reading from the file
   */
  public byte[] readAllBytes(String path) throws IOException {
    try (InputStream is = read(path)) {
      return is.readAllBytes();
    } catch (S3ClientException e) {
      throw log.throwing(new IOException("Error reading file with path: " + path, e));
    }
  }

  /**
   * Read all lines from a file as a {@code List}
   *
   * @param path - the path to the file on S3-compatible storage
   * @return the lines from the file as a {@code List}
   * @throws IOException - if an I/O error occurs reading from the file
   */
  public List<String> readAllLines(String path) throws IOException {
    try (InputStream is = read(path);
         InputStreamReader isr = new InputStreamReader(is);
         BufferedReader br = new BufferedReader(isr)) {
      return br.lines()
        .toList();
    } catch (S3ClientException e) {
      throw log.throwing(new IOException("Error reading file: " + path, e));
    }
  }

  /**
   * Read some lines from a file as a {@code List}
   *
   * @param path  - the path to the file on S3-compatible storage
   * @param skip  - the number of lines to skip from start of file
   * @param limit - the number of lines to read after skipping
   * @return the lines from the file as a {@code List}
   * @throws IOException - if an I/O error occurs reading from the file
   */
  public List<String> readLines(String path, int skip, int limit) throws IOException {
    try (InputStream is = read(path);
         InputStreamReader isr = new InputStreamReader(is);
         BufferedReader br = new BufferedReader(isr)) {
      return br.lines()
        .skip(skip)
        .limit(limit)
        .toList();
    } catch (S3ClientException e) {
      throw log.throwing(new IOException("Error reading file: " + path, e));
    }
  }

  /**
   * Count the lines in a file
   *
   * @param path - the path to the file on S3-compatible storage
   * @return the number of lines in the file
   * @throws IOException - if an I/O error occurs reading from the file
   */
  public long numLines(String path) throws IOException {
    try (InputStream is = read(path);
         InputStreamReader isr = new InputStreamReader(is);
         BufferedReader br = new BufferedReader(isr)) {
      return br.lines()
        .count();
    } catch (S3ClientException e) {
      throw log.throwing(new IOException("Error reading file: " + path, e));
    }
  }

  /**
   * Read a number of non-blank lines from a file as a {@code Stream}
   *
   * @param path - the path to the file on S3-compatible storage
   * @param num  - the num of lines to read from start of file
   * @return the lines from the file as a {@code Stream}
   * @throws IOException - if an I/O error occurs reading from the file
   */
  public List<String> linesNumber(String path, int num) throws IOException {
    try (InputStream is = read(path);
         InputStreamReader isr = new InputStreamReader(is);
         BufferedReader reader = new BufferedReader(isr)) {
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

  public void removeRecursive(String path) {
    try {
      remove(listRecursive(path).toArray(s -> new String[s]));
    } catch (S3ClientException e) {
      log.error("Cannot delete file: {}", path, e);
    }
  }

  /**
   * Handle leading slashes in a provided path, if there is one. For legacy reasons, some paths for
   * temporary local files may start with a slash, as they use filesystem-based naming. To deal with
   * this, we append {@code TMP_PATH_PREFIX} to the start of such paths. These paths should never
   * appear in completed job result filenames.
   *
   * @param path
   * @return
   */
  protected String sanitizePath(String path) {
    if (path.startsWith(PATH_SEPARATOR)) {
      String result = (TMP_PATH_PREFIX + path).replaceAll(PATH_SEPARATOR + "+", PATH_SEPARATOR);
      log.info("Prefixed path '{}' for S3 operations, converted to '{}'", path, result);
      return result;
    }
    return path;
  }
}
