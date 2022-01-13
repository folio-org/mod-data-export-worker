package org.folio.dew.repository;

import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.DownloadObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteArgs;
import io.minio.ObjectWriteResponse;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.UploadObjectArgs;
import io.minio.credentials.IamAwsProvider;
import io.minio.credentials.Provider;
import io.minio.credentials.StaticProvider;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.config.properties.MinIoProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Repository;

@Repository
@Log4j2
public class MinIOObjectStorageRepository {

  private static final String CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME = "attachment";
  private static final String CONTENT_DISPOSITION_HEADER_WITH_FILENAME = CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME + "; filename=\"%s\"";

  private final MinioClient client;
  private final String bucket;

  public MinIOObjectStorageRepository(MinIoProperties properties) {
    final String accessKey = properties.getAccessKey();
    final String endpoint = properties.getEndpoint();
    final String region = properties.getRegion();
    final String secretKey = properties.getSecretKey();
    log.info("Creating MinIO client endpoint {},region {},bucket {},accessKey {},secretKey {}.", endpoint, region, properties.getBucket(),
        StringUtils.isNotBlank(accessKey) ? "<set>" : "<not set>", StringUtils.isNotBlank(secretKey) ? "<set>" : "<not set>");

    var builder = MinioClient.builder().endpoint(endpoint);
    if (StringUtils.isNotBlank(region)) {
      builder.region(region);
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

    this.bucket = properties.getBucket();
  }

  @SneakyThrows
  public void createBucketIfNotExists() {
    if (StringUtils.isNotBlank(bucket) && !client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
      client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
      log.info("Created {} bucket.", bucket);
    }
  }

  public ObjectWriteResponse uploadObject(String object, String filename, String downloadFilename, String contentType, boolean isSourceShouldBeDeleted)
      throws IOException, ServerException, InsufficientDataException, InternalException, InvalidResponseException,
      InvalidKeyException, NoSuchAlgorithmException, XmlParserException, ErrorResponseException {
    log.info("Uploading object {},filename {},downloadFilename {},contentType {}.", object, filename, downloadFilename,
        contentType);
    ObjectWriteResponse result = client.uploadObject(
        createArgs(UploadObjectArgs.builder().filename(filename), object, downloadFilename, contentType));

    if (isSourceShouldBeDeleted) {
      FileUtils.deleteQuietly(new File(filename));
    }

    log.info("Deleted temp file {}.", filename);

    return result;
  }

  public void downloadObject(String objectToGet, String fileToSave) throws IOException, InvalidKeyException,
    InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException,
    InternalException, XmlParserException, ErrorResponseException {
    client.downloadObject(DownloadObjectArgs.builder().bucket(bucket).object(objectToGet).filename(fileToSave).build());
  }

  public ObjectWriteResponse composeObject(String destObject, List<String> sourceObjects, String downloadFilename,
      String contentType)
      throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException,
      ServerException, InternalException, XmlParserException, ErrorResponseException {
    List<ComposeSource> sources = sourceObjects.stream()
        .map(so -> ComposeSource.builder().bucket(bucket).object(so).build())
        .collect(Collectors.toList());
    log.info("Composing object {},sources [{}],downloadFilename {},contentType {}.", destObject,
        sources.stream().map(s -> String.format("bucket %s,object %s", s.bucket(), s.object())).collect(Collectors.joining(",")),
        downloadFilename, contentType);
    ObjectWriteResponse result = client.composeObject(
        createArgs(ComposeObjectArgs.builder().sources(sources), destObject, downloadFilename, contentType));

    removeObjects(sourceObjects);

    return result;
  }

  public Iterable<Result<DeleteError>> removeObjects(List<String> objects) {
    log.info("Deleting objects [{}].", StringUtils.join(objects, ","));
    return client.removeObjects(RemoveObjectsArgs.builder()
        .bucket(bucket)
        .objects(objects.stream().map(DeleteObject::new).collect(Collectors.toList()))
        .build());
  }

  public String objectWriteResponseToPresignedObjectUrl(ObjectWriteResponse response)
      throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException,
      ServerException, InternalException, XmlParserException, ErrorResponseException {
    String result = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
        .method(Method.GET)
        .bucket(response.bucket())
        .object(response.object())
        .region(response.region())
        .versionId(response.versionId())
        .build());
    log.info("Created presigned URL {}.", result);
    return result;
  }

  private <T extends ObjectWriteArgs, B extends ObjectWriteArgs.Builder<B, T>> T createArgs(B builder, String object,
      String downloadFilename, String contentType) {
    Map<String, String> headers = new HashMap<>(2);
    if (StringUtils.isNotBlank(downloadFilename)) {
      headers.put(HttpHeaders.CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_HEADER_WITH_FILENAME, downloadFilename));
    } else {
      headers.put(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME);
    }
    if (StringUtils.isNotBlank(contentType)) {
      headers.put(HttpHeaders.CONTENT_TYPE, contentType);
    }
    return builder.headers(headers).object(object).bucket(bucket).build();
  }

}
