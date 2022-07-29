package org.folio.dew.repository;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.minio.messages.Item;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.config.properties.FsToMinIoAdapterProperties;

import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.credentials.IamAwsProvider;
import io.minio.credentials.Provider;
import io.minio.credentials.StaticProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;

/**
 * Local FS to S3-compatible storage adapter.
 */
@Repository
@Log4j2
public class LocalFsToMinIoAdapter {

  private final MinioClient client;
  private final String bucket;
  private final String region;

  public LocalFsToMinIoAdapter(FsToMinIoAdapterProperties properties) {
    final String accessKey = properties.getAccessKey();
    final String endpoint = properties.getEndpoint();
    final String regionName = properties.getRegion();
    final String bucketName = properties.getBucket();
    final String secretKey = properties.getSecretKey();
    log.info("Creating MinIO client endpoint {},region {},bucket {},accessKey {},secretKey {}.", endpoint, regionName, bucketName,
        StringUtils.isNotBlank(accessKey) ? "<set>" : "<not set>", StringUtils.isNotBlank(secretKey) ? "<set>" : "<not set>");

    var builder = MinioClient.builder().endpoint(endpoint);
    if (StringUtils.isNotBlank(regionName)) {
      builder.region(regionName);
    }

    Provider provider;
    if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
      provider = new StaticProvider(accessKey, secretKey, null);
    } else {
      provider = new IamAwsProvider(null, null);
    }
    log.info("{} MinIO credentials provider created.", provider.getClass().getSimpleName());
    builder.credentialsProvider(provider);

    client = builder.build();

    this.bucket = bucketName;
    this.region = regionName;

    createBucketIfNotExists();
  }

  /**
   * Writes bytes to a file on S3-compatible storage
   *
   * @param path - the path to the file on S3-compatible storage
   * @param bytes – the byte array with the bytes to write
   * @return the path to the file
   * @throws IOException - if an I/O error occurs
   */
  public String write(String path, byte[] bytes) throws IOException {
    try(var is = new ByteArrayInputStream(bytes)) {
      return client.putObject(PutObjectArgs.builder()
          .bucket(bucket)
          .region(region)
          .object(path)
          .stream(is, bytes.length, -1)
          .build())
        .object();
    } catch (Exception e) {
      throw new IOException("Cannot write file: " + path, e);
    }
  }

  /**
   * Appends byte[] to existing on the storage file.
   *
   * @param path - the path to the file on S3-compatible storage
   * @param bytes - the byte array with the bytes to write
   * @throws IOException if an I/O error occurs
   */
  public void append(String path, byte[] bytes) throws IOException {

    try {

      var size = client.statObject(StatObjectArgs.builder()
        .bucket(bucket)
        .region(region)
        .object(path).build()).size();

      if (size > ObjectWriteArgs.MIN_MULTIPART_SIZE) {
        var temporaryFileName = path + "_temp";
        write(temporaryFileName, bytes);

        client.composeObject(ComposeObjectArgs.builder()
          .bucket(bucket)
          .region(region)
          .object(path)
          .sources(List.of(ComposeSource.builder()
              .bucket(bucket)
              .region(region)
              .object(path)
              .build(),
            ComposeSource.builder()
              .bucket(bucket)
              .region(region)
              .object(temporaryFileName)
              .build()))
          .build());

        delete(temporaryFileName);

      } else {
        var original = readAllBytes(path);
        byte[] composed = new byte[original.length + bytes.length];
        System.arraycopy(original, 0, composed, 0, original.length);
        System.arraycopy(bytes, 0, composed, original.length, bytes.length);
        write(path, composed);
      }
    } catch (Exception e) {
      throw new IOException("Cannot append data", e);
    }
  }

  /**
   * Deletes a file
   *
   * @param path - the path to the file to delete
   * @throws IOException if an I/O error occurs
   */
  public void delete(String path) throws IOException {
    try {
      client.removeObject(RemoveObjectArgs.builder()
        .bucket(bucket)
        .region(region)
        .object(path)
        .build());
    } catch (Exception e) {
      throw new IOException("Cannot delete file: " + path, e);
    }
  }

  /**
   * Return a {@code Stream} that is lazily populated with {@code
   * Path} by walking the file tree rooted at a given starting file/folder.
   *
   * @param path - the path to the folder
   * @return the {@link Stream} of {@link Path} of the nested files
   * @throws IOException if an I/O error occurs
   */
  public Stream<String> list(String path) throws IOException {
    return getInternalStructure(path, false);
  }

  /**
   * Return a {@code Stream} that is lazily populated with {@code
   * Path} by walking the file tree rooted at a given starting file.
   *
   * @param path - the path to the folder
   * @return the {@link Stream} of {@link Path} of the nested files
   * @throws IOException if an I/O error occurs
   */
  public Stream<String> walk(String path) throws IOException {
    return getInternalStructure(path, true);
  }

  /**
   * Verifies if file exists on storage
   *
   * @param path - the path to the file on S3-compatible storage
   * @return true if file exists, otherwise - false
   */
  public boolean exists(String path) throws IOException {
    var iterator = client.listObjects(ListObjectsArgs.builder()
      .bucket(bucket)
      .region(region)
      .prefix(path)
      .maxKeys(1)
      .build())
      .iterator();
    try {
      return iterator.hasNext() && Objects.nonNull(iterator.next().get());
    } catch (Exception e) {
      throw new IOException("Error file existing verification", e);
    }
  }

  /**
   * Verifies if file doesn't exist on S3-compatible storage
   * @param path - the path to the file
   * @return true if file doesn't exist, otherwise - false
   */
  public boolean notExists(String path) throws IOException {
    return !exists(path);
  }

  /**
   * Opens a file, returning an input stream to read from the file
   *
   * @param path - the path to the file on S3-compatible storage
   * @return a new input stream
   * @throws IOException - if an I/O error occurs reading from the file
   */
  public InputStream newInputStream(String path) throws IOException {
    try {
      return client.getObject(GetObjectArgs.builder()
        .bucket(bucket)
        .region(region)
        .object(path)
        .build());
    } catch (Exception e) {
      throw new IOException("Error creating input stream", e);
    }
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
      return  is.readAllBytes();
    } catch (Exception e) {
      throw new IOException("Error reading file", e);
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
   * Read all lines from a file
   *
   * @param path - the path to the file on S3-compatible storage
   * @return
  the lines from the file as a List
   * @throws IOException - if an I/O error occurs reading from the file
   */
  public List<String> readAllLines(String path) throws IOException {
    try {
      return lines(path).collect(Collectors.toList());
    } catch(Exception e) {
      throw new IOException("Error reading file", e);
    }
  }

  private void createBucketIfNotExists() {
    try {
      if (StringUtils.isNotBlank(bucket) && !client.bucketExists(BucketExistsArgs.builder().bucket(bucket).region(region).build())) {
        client.makeBucket(MakeBucketArgs.builder()
          .bucket(bucket)
          .region(region)
          .build());
        log.info("Created {} bucket.", bucket);
      } else {
        log.info("Bucket has already exist.");
      }
    } catch(Exception e) {
      log.error("Error creating bucket: " + bucket, e.getMessage());
    }
  }

  private Stream<String> getInternalStructure(String path, boolean isRecursive) throws IOException {
    try {
      return StreamSupport.stream(client.listObjects(ListObjectsArgs.builder()
        .bucket(bucket)
        .region(region)
        .prefix(path)
        .recursive(isRecursive)
        .build()).spliterator(), false).map(item -> {
        try {
          return item.get().objectName();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    } catch(Exception e) {
      throw new IOException("Cannot delete file: " + path, e);
    }
  }
}
