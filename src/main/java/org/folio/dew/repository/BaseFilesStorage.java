package org.folio.dew.repository;

import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.credentials.IamAwsProvider;
import io.minio.credentials.Provider;
import io.minio.credentials.StaticProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.config.properties.MinioClientProperties;
import org.folio.dew.error.FileOperationException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.minio.ObjectWriteArgs.MIN_MULTIPART_SIZE;
import static java.lang.String.format;

@Log4j2
public class BaseFilesStorage implements S3CompatibleStorage {

  private final MinioClient client;
  private S3Client s3Client;
  private final String bucket;
  private final String region;

  private final boolean isComposeWithAwsSdk;

  public BaseFilesStorage(MinioClientProperties properties) {
    final String accessKey = properties.getAccessKey();
    final String endpoint = properties.getEndpoint();
    final String regionName = properties.getRegion();
    final String bucketName = properties.getBucket();
    final String secretKey = properties.getSecretKey();
    isComposeWithAwsSdk = properties.isComposeWithAwsSdk();
    log.info("Creating MinIO client endpoint {},region {},bucket {},accessKey {},secretKey {}, isComposedWithAwsSdk {}.", endpoint, regionName, bucketName,
      StringUtils.isNotBlank(accessKey) ? "<set>" : "<not set>", StringUtils.isNotBlank(secretKey) ? "<set>" : "<not set>", isComposeWithAwsSdk);

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

    if (isComposeWithAwsSdk) {
      AwsCredentialsProvider credentialsProvider;

      if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
        var awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
        credentialsProvider = StaticCredentialsProvider.create(awsCredentials);
      } else {
        credentialsProvider = DefaultCredentialsProvider.create();
      }

      s3Client = S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(regionName))
        .credentialsProvider(credentialsProvider)
        .build();
    }

  }

  public MinioClient getMinioClient() {
    return client;
  }

  public void createBucketIfNotExists() {
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
      log.error("Error creating bucket: " + bucket, e);
    }
  }

  /**
   * Upload file on S3-compatible storage
   *
   * @param path - the path to the file on S3-compatible storage
   * @param filename – path to uploaded file
   * @return the path to the file
   * @throws IOException - if an I/O error occurs
   */
  public String upload(String path, String filename) throws IOException {
    try {
      return client.uploadObject(UploadObjectArgs.builder()
          .bucket(bucket)
          .region(region)
          .object(path)
          .filename(filename)
          .build())
        .object();
    } catch (Exception e) {
      throw new IOException("Cannot upload file: " + path, e);
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

    if (isComposeWithAwsSdk) {
      log.info("Writing with using AWS SDK client");
      s3Client.putObject(PutObjectRequest.builder().bucket(bucket)
          .key(path).build(),
        RequestBody.fromBytes(bytes));
      return path;
    } else {
      log.info("Writing with using Minio client");
      try(var is = new ByteArrayInputStream(bytes)) {
        return client.putObject(PutObjectArgs.builder()
            .bucket(bucket)
            .region(region)
            .object(path)
            .headers(headers)
            .stream(is, -1, MIN_MULTIPART_SIZE)
            .build())
          .object();
      } catch (Exception e) {
        throw new IOException("Cannot write file: " + path, e);
      }

    }
  }

  public String write(String path, byte[] bytes) throws IOException {
    return write(path, bytes, new HashMap<>());
  }

  /**
   * Writes file to a file on S3-compatible storage
   *
   * @param path - the path to the file on S3-compatible storage
   * @param inputPath – path to the file to write
   * @param headers - headers
   * @return the path to the file
   * @throws IOException - if an I/O error occurs
   */
  public String writeFile(String path, Path inputPath, Map<String, String> headers) throws IOException {

    if (isComposeWithAwsSdk) {
      log.info("Writing file using AWS SDK client");
      s3Client.putObject(PutObjectRequest.builder().bucket(bucket)
          .key(path).build(),
        RequestBody.fromFile(inputPath));
      return path;
    } else {
      log.info("Writing file using Minio client");
      try (var is = Files.newInputStream(inputPath)) {
        return client.putObject(PutObjectArgs.builder()
            .bucket(bucket)
            .region(region)
            .object(path)
            .headers(headers)
            .stream(is, -1, MIN_MULTIPART_SIZE)
            .build())
          .object();
      } catch (Exception e) {
        throw new IOException("Cannot write file: " + path, e);
      }

    }
  }

  public String writeFile(String destPath, Path inputPath) throws IOException {
    return writeFile(destPath, inputPath, new HashMap<>());
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
      if (notExists(path)) {
        log.info("Appending non-existing file");
        write(path, bytes);
      } else {
        var size = client.statObject(StatObjectArgs.builder()
          .bucket(bucket)
          .region(region)
          .object(path).build()).size();

        log.info("Appending to {} with size {}", path, size);
        if (size > MIN_MULTIPART_SIZE) {

          if (isComposeWithAwsSdk) {

            var createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
              .bucket(bucket)
              .key(path)
              .build();

            var uploadId = s3Client.createMultipartUpload(createMultipartUploadRequest).uploadId();

            var uploadPartRequest1 = UploadPartCopyRequest.builder()
              .sourceBucket(bucket)
              .sourceKey(path)
              .uploadId(uploadId)
              .destinationBucket(bucket)
              .destinationKey(path)
              .partNumber(1).build();

            var uploadPartRequest2 = UploadPartRequest.builder()
              .bucket(bucket)
              .key(path)
              .uploadId(uploadId)
              .partNumber(2).build();

            var originalEtag  = s3Client.uploadPartCopy(uploadPartRequest1).copyPartResult().eTag();
            var appendedEtag = s3Client.uploadPart(uploadPartRequest2, RequestBody.fromBytes(bytes)).eTag();

            var original = CompletedPart.builder()
              .partNumber(1)
              .eTag(originalEtag).build();
            var appended = CompletedPart.builder()
              .partNumber(2)
              .eTag(appendedEtag).build();

            var completedMultipartUpload = CompletedMultipartUpload.builder()
              .parts(original, appended)
              .build();

            var completeMultipartUploadRequest =
              CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(path)
                .uploadId(uploadId)
                .multipartUpload(completedMultipartUpload)
                .build();

            s3Client.completeMultipartUpload(completeMultipartUploadRequest);

          } else {

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

          }

        } else {
          write(path, ArrayUtils.addAll(readAllBytes(path), bytes));
        }
      }
    } catch (Exception e) {
      throw new IOException("Cannot append data for path: " + path, e);
    }
  }

  /**
   * Deletes a file
   *
   * @param path - the path to the file to delete
   * @throws FileOperationException if an I/O error occurs
   */
  public void delete(String path) {
    try {
      var paths = walk(path).collect(Collectors.toList());

      paths.forEach(p -> {
        try {
          client.removeObject(RemoveObjectArgs.builder()
            .bucket(bucket)
            .region(region)
            .object(p)
            .build());
        } catch (Exception e) {
          log.error(format("Cannot delete file: %s", p), e.getMessage());
        }
      });
    } catch (Exception e) {
      throw new FileOperationException("Cannot delete file: " + path, e);
    }
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
    return getInternalStructure(path, true);
  }

  /**
   * Verifies if file exists on storage
   *
   * @param path - the path to the file on S3-compatible storage
   * @return true if file exists, otherwise - false
   */
  public boolean exists(String path)  {
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
      log.error("Error file existing verification, path: " + path, e);
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
      throw new IOException("Error creating input stream for path: " + path, e);
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

  /**
   * Read all lines from a file
   *
   * @param path - the path to the file on S3-compatible storage
   * @return
  the lines from the file as a List
   * @throws IOException - if an I/O error occurs reading from the file
   */
  public List<String> readAllLines(String path) throws IOException {
    try (var lines = lines(path)) {
      return lines.collect(Collectors.toList());
    } catch(Exception e) {
      throw new IOException("Error reading file: " + path, e);
    }
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
          BaseFilesStorage.this.write(path, buffer);
        } catch (IOException e) {
          throw new FileOperationException("Error closing stream and writes bytes to path: " + path, e);
        } finally {
          buffer = new byte[0];
        }
      }
    };
  }

  public BufferedWriter writer(String path) {
    return new BufferedWriter(new OutputStreamWriter(newOutputStream(path)));
  }

  private Stream<String> getInternalStructure(String path, boolean isRecursive)  {
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
          throw new FileOperationException(e);
        }
      });
    } catch(Exception e) {
      log.error("Cannot read folder: " + path, e);
      return null;
    }
  }
}
