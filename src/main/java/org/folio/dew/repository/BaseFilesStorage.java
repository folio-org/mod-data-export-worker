package org.folio.dew.repository;


import io.minio.http.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.config.properties.MinioClientProperties;
import org.folio.dew.error.FileOperationException;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.PutObjectAdditionalOptions;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.folio.s3.exception.S3ClientException;
import org.springframework.http.HttpHeaders;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;


@Log4j2
public class BaseFilesStorage implements S3CompatibleStorage {

  public static final String CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME = "attachment";
  public static final String CONTENT_DISPOSITION_HEADER_WITH_FILENAME = CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME + "; filename=\"%s\"";


  private static final String SET_VALUE = "<set>";
  private static final String NOT_SET_VALUE = "<not set>";
  final FolioS3Client client;
  private final String subPath;
  final String bucket;
  private final int urlExpirationTimeInSeconds;

  public BaseFilesStorage(MinioClientProperties properties) {

    bucket = properties.getBucket();
    urlExpirationTimeInSeconds = properties.getUrlExpirationTimeInSeconds();
    subPath = properties.getSubPath();

    final String accessKey = properties.getAccessKey();
    final String endpoint = properties.getEndpoint();
    final String regionName = properties.getRegion();
    final String secretKey = properties.getSecretKey();
    boolean isComposeWithAwsSdk = properties.isComposeWithAwsSdk();
    final boolean isForcePathStyle = properties.isForcePathStyle();

    log.info("Creating S3 client endpoint {},region {},bucket {},accessKey {},secretKey {}, subPath {}, isComposedWithAwsSdk {}.", endpoint, regionName, bucket,
        StringUtils.isNotBlank(accessKey) ? SET_VALUE : NOT_SET_VALUE, StringUtils.isNotBlank(secretKey) ? SET_VALUE : NOT_SET_VALUE,
        StringUtils.isNotBlank(subPath) ? SET_VALUE : NOT_SET_VALUE, isComposeWithAwsSdk);

    client = S3ClientFactory.getS3Client(S3ClientProperties.builder()
        .endpoint(endpoint)
        .secretKey(secretKey)
        .accessKey(accessKey)
        .bucket(bucket)
        .awsSdk(isComposeWithAwsSdk)
        .forcePathStyle(isForcePathStyle)
        .subPath(subPath)
        .region(regionName)
        .build());

    try {
      client.createBucketIfNotExists();
    } catch (S3ClientException e) {
      log.error("Error creating bucket: {} during RemoteStorageClient initialization", bucket);
    }
  }

  /**
   * Writes bytes to a file on S3-compatible storage
   *
   * @param path - the path to the file on S3-compatible storage
   * @param bytes – the byte array with the bytes to write
   * @param headers - headers
   * @return the path to the file
   * @throws IOException - if an I/O error occurs
   */
  public String write(String path, byte[] bytes, Map<String, String> headers) throws IOException {
    var options = PutObjectAdditionalOptions.builder()
        .contentDisposition(headers.get(HttpHeaders.CONTENT_DISPOSITION))
        .contentType(headers.get(HttpHeaders.CONTENT_TYPE))
        .build();
    return client.write(path, new ByteArrayInputStream(bytes), bytes.length, options);
  }

  public String write(String path, byte[] bytes) throws IOException {
    return write(path, bytes, new HashMap<>());
  }

  /**
   * Deletes a file
   *
   * @param path - the path to the file to delete
   * @throws FileOperationException if an I/O error occurs
   */
  public void delete(String path) {
    client.remove(walk(path).toArray(String[]::new));
  }

  /**
   * Deletes a file
   *
   * @param objects - the path to the file to delete
   * @throws FileOperationException if an I/O error occurs
   */
  public void delete(List<String> objects) {
    client.remove(objects.toArray(String[]::new));
  }

  /**
   * Return a {@code Stream} that is lazily populated with {@code
   * Path} by walking the file tree rooted at a given starting file.
   *
   * @param path - the path to the folder
   * @return the {@link Stream}  of the nested files
   * @throws FileOperationException if an I/O error occurs
   */
  public Stream<String> walk(String path) {
    return getInternalStructure(path);
  }

  /**
   * Verifies if file exists on storage
   *
   * @param path - the path to the file on S3-compatible storage
   * @return true if file exists, otherwise - false
   */
  public boolean exists(String path)  {
    try {
      var paths =  client.list(path);
      return !paths.isEmpty() && Objects.nonNull(paths.getFirst());
    } catch (Exception e) {
      log.error("Error file existing verification, path: {}", path, e);
      return false;
    }
  }

  /**
   * Verifies if file doesn't exist on S3-compatible storage
   * @param path - the path to the file
   * @return true if file doesn't exist, otherwise - false
   */
  public boolean notExists(String path) {
    return !exists(path);
  }

  /**
   * Opens a file, returning an input stream to read from the file
   *
   * @param path - the path to the file on S3-compatible storage
   * @return a new input stream
   */
  public InputStream newInputStream(String path) {
    return client.read(path);
  }

  /**
   * Reads all the bytes from a file
   *
   * @param path - the path to the file on S3-compatible storage
   * @return a byte array containing the bytes read from the file
   * @throws IOException - if an I/O error occurs reading from the file
   */
  public byte[] readAllBytes(String path) throws IOException {
    try (var is = newInputStream(path)) {
      return is.readAllBytes();
    } catch (Exception e) {
      throw new IOException("Error reading file with path: " + path, e);
    }
  }

  /**
   * Read all lines from a file as a {@code Stream}
   *
   * @param path - the path to the file on S3-compatible storage
   * @return the lines from the file as a {@code Stream}
   * @throws IOException - if an I/O error occurs reading from the file
   */
  public Stream<String> lines(String path) throws IOException {
    return new BufferedReader(new InputStreamReader(newInputStream(path))).lines();
  }

  /**
   * Read number lines from a file as a {@code Stream}
   *
   * @param path - the path to the file on S3-compatible storage
   * @param num - the num of lines to read from start of file
   * @return the lines from the file as a {@code Stream}
   * @throws IOException - if an I/O error occurs reading from the file
   */
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

  public String compose(String destObject, List<String> sourceObjects, String downloadFilename,
      String contentType) {

    var headers = prepareHeaders(downloadFilename, contentType);
    var result = client.compose(destObject, sourceObjects, PutObjectAdditionalOptions.builder()
        .contentType(headers.get(HttpHeaders.CONTENT_TYPE))
        .contentDisposition(HttpHeaders.CONTENT_DISPOSITION).build());

    client.remove(sourceObjects.toArray(new String[0]));
    return result;
  }

  public String objectToPresignedObjectUrl(String object) {
    return client.getPresignedUrl(object, Method.GET, urlExpirationTimeInSeconds, TimeUnit.SECONDS);
  }

  Map<String, String> prepareHeaders(String downloadFilename, String contentType) {
    Map<String, String> headers = HashMap.newHashMap(2);
    if (StringUtils.isNotBlank(downloadFilename)) {
      headers.put(HttpHeaders.CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_HEADER_WITH_FILENAME, downloadFilename));
    } else {
      headers.put(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME);
    }
    if (StringUtils.isNotBlank(contentType)) {
      headers.put(HttpHeaders.CONTENT_TYPE, contentType);
    }
    return headers;
  }

  private Stream<String> getInternalStructure(String path)  {
    return client.listRecursive(path).stream();
  }
}
