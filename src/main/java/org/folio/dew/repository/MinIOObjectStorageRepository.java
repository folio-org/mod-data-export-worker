package org.folio.dew.repository;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Log4j2
public class MinIOObjectStorageRepository {

  private static final String CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME = "attachment";
  private static final String CONTENT_DISPOSITION_HEADER_WITH_FILENAME = CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME + "; filename=\"%s\"";

  private final MinioClient client;
  @Value("${minio.workspaceBucketName}")
  private String workspaceBucketName;

  public MinIOObjectStorageRepository(@Value("${minio.url}") String url, @Value("${minio.accessKey}") String accessKey,
      @Value("${minio.secretKey}") String secretKey)
      throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException,
      ServerException, InternalException, XmlParserException, ErrorResponseException {
    MinioClient.Builder builder = MinioClient.builder().endpoint(url);
    if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
      builder.credentials(accessKey, secretKey);
    }
    client = builder.build();

    if (StringUtils.isBlank(workspaceBucketName)) {
      log.info("Working bucket /.");
      return;
    }
    if (client.bucketExists(BucketExistsArgs.builder().bucket(workspaceBucketName).build())) {
      log.info("Working bucket {}.", workspaceBucketName);
    } else {
      client.makeBucket(MakeBucketArgs.builder().bucket(workspaceBucketName).build());
      log.info("Created working bucket {}.", workspaceBucketName);
    }
  }

  public ObjectWriteResponse uploadObject(String object, String filename, String downloadFilename, String contentType)
      throws IOException, ServerException, InsufficientDataException, InternalException, InvalidResponseException,
      InvalidKeyException, NoSuchAlgorithmException, XmlParserException, ErrorResponseException {
    log.info("Uploading object {},filename {},downloadFilename {},contentType {}.", object, filename, downloadFilename,
        contentType);
    ObjectWriteResponse result = client.uploadObject(
        createArgs(UploadObjectArgs.builder().filename(filename), object, downloadFilename, contentType));

    new File(filename).delete();
    log.info("Temp file {} deleted.", filename);

    return result;
  }

  public ObjectWriteResponse composeObject(String destObject, List<String> sourceObjects, String downloadFilename,
      String contentType)
      throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException,
      ServerException, InternalException, XmlParserException, ErrorResponseException {
    List<ComposeSource> sources = sourceObjects.stream()
        .map(so -> ComposeSource.builder().bucket(workspaceBucketName).object(so).build())
        .collect(Collectors.toList());
    log.info("Composing object {},sources [{}],downloadFilename {},contentType {}.", destObject,
        sources.stream().map(s -> String.format("bucket %s object %s", s.bucket(), s.object())).collect(Collectors.joining(",")),
        downloadFilename, contentType);
    ObjectWriteResponse result = client.composeObject(
        createArgs(ComposeObjectArgs.builder().sources(sources), destObject, downloadFilename, contentType));

    removeObjects(sourceObjects);

    return result;
  }

  public Iterable<Result<DeleteError>> removeObjects(List<String> objects) {
    log.info("Deleting objects [{}].", StringUtils.join(objects, ","));
    return client.removeObjects(RemoveObjectsArgs.builder()
        .bucket(workspaceBucketName)
        .objects(objects.stream().map(DeleteObject::new).collect(Collectors.toList()))
        .build());
  }

  public String objectWriteResponseToPresignedObjectUrl(ObjectWriteResponse response)
      throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException,
      ServerException, InternalException, XmlParserException, ErrorResponseException {
    String result = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET).bucket(response.bucket()).
        object(response.object()).region(response.region()).versionId(response.versionId()).build());
    log.info("Created presigned S3 URL {}.", result);
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
    return builder.headers(headers).object(object).bucket(workspaceBucketName).build();
  }

}
