package org.folio.dew.repository;

import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteArgs;
import io.minio.ObjectWriteResponse;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.minio.messages.Item;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.config.properties.RemoteFilesStorageProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Repository;


@Repository
@Log4j2
public class RemoteFilesStorage extends BaseFilesStorage {

  public static final String CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME = "attachment";
  public static final String CONTENT_DISPOSITION_HEADER_WITH_FILENAME = CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME + "; filename=\"%s\"";

  private final MinioClient client;
  @Autowired
  private LocalFilesStorage localFilesStorage;
  private final String bucket;
  private final String region;
  private boolean isComposeWithAwsSdk;

  public RemoteFilesStorage(RemoteFilesStorageProperties properties) {
    super(properties);
    this.bucket = properties.getBucket();
    this.region = properties.getRegion();
    this.client = getMinioClient();
    isComposeWithAwsSdk = properties.isComposeWithAwsSdk();
  }

  public String uploadObject(String object, String filename, String downloadFilename, String contentType, boolean isSourceShouldBeDeleted)
      throws IOException {
    log.info("Uploading object {},filename {},downloadFilename {},contentType {}.", object, filename, downloadFilename,
        contentType);

    var result = write(object, localFilesStorage.readAllBytes(filename), prepareHeaders(downloadFilename, contentType));

    if (isSourceShouldBeDeleted) {
      localFilesStorage.delete(filename);
      log.info("Deleted temp file {}.", filename);
    }

    return result;
  }

  public void downloadObject(String objectToGet, String fileToSave) throws IOException, InvalidKeyException,
    InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException,
    InternalException, XmlParserException, ErrorResponseException {
    localFilesStorage.write(fileToSave, client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectToGet).build()).readAllBytes());
  }

  public boolean containsFile(String fileName)
    throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
    InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    for (Result<Item> itemResult : client.listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(fileName).build())) {
      if (fileName.equals(itemResult.get().objectName())) {
        return true;
      }
    }
    return false;
  }

  public String composeObject(String destObject, List<String> sourceObjects, String downloadFilename,
      String contentType)
      throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException,
      ServerException, InternalException, XmlParserException, ErrorResponseException {
    List<ComposeSource> sources = sourceObjects.stream()
        .map(so -> ComposeSource.builder().bucket(bucket).object(so).build())
        .collect(Collectors.toList());
    log.info("Composing object {},sources [{}],downloadFilename {},contentType {}.", destObject,
        sources.stream().map(s -> String.format("bucket %s,object %s", s.bucket(), s.object())).collect(Collectors.joining(",")),
        downloadFilename, contentType);
    var result = client.composeObject(
        createArgs(ComposeObjectArgs.builder().sources(sources), destObject, downloadFilename, contentType)).object();

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

  public String objectToPresignedObjectUrl(String object)
    throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException,
    ServerException, InternalException, XmlParserException, ErrorResponseException {
    String result = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
      .method(Method.GET)
      .bucket(bucket)
      .object(object)
      .region(region)
      .build());
    log.info("Created presigned URL {}.", result);
    return result;
  }

  private <T extends ObjectWriteArgs, B extends ObjectWriteArgs.Builder<B, T>> T createArgs(B builder, String object,
      String downloadFilename, String contentType) {
    Map<String, String> headers = prepareHeaders(downloadFilename, contentType);
    return builder.headers(headers).object(object).bucket(bucket).build();
  }

  private Map<String, String> prepareHeaders(String downloadFilename, String contentType) {
    Map<String, String> headers = new HashMap<>(2);
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

}
