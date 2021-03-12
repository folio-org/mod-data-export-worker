package org.folio.dew.repository;

import io.minio.*;
import io.minio.errors.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Log4j2
public class MinIOObjectStorageRepository {

  private final MinioClient minioClient;
  @Value("${minio.workspaceBucketName}")
  private String workspaceBucketName;

  public MinIOObjectStorageRepository(@Value("${minio.url}") String url, @Value("${minio.accessKey}") String accessKey,
      @Value("${minio.secretKey}") String secretKey) {
    MinioClient.Builder builder = MinioClient.builder().endpoint(url);
    if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
      builder.credentials(accessKey, secretKey);
    }
    minioClient = builder.build();
  }

  public ObjectWriteResponse uploadObject(String objectName, String filePath, String contentType)
      throws IOException, ServerException, InsufficientDataException, InternalException, InvalidResponseException,
      InvalidKeyException, NoSuchAlgorithmException, XmlParserException, ErrorResponseException {
    UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
        .bucket(workspaceBucketName)
        .object(objectName)
        .filename(filePath)
        .contentType(contentType)
        .build();

    log.info("Uploading object {} filename {} contentType {}.", objectName, filePath, contentType);
    return minioClient.uploadObject(uploadObjectArgs);
  }

  public ObjectWriteResponse composeObject(String destObjectName, List<String> sourceObjectNames)
      throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException,
      ServerException, InternalException, XmlParserException, ErrorResponseException {
    List<ComposeSource> sourceObjects = new ArrayList<>();

    for (String name : sourceObjectNames) {
      ComposeSource composeSource = ComposeSource.builder().bucket(workspaceBucketName).object(name).build();
      sourceObjects.add(composeSource);
    }

    ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
        .bucket(workspaceBucketName)
        .object(destObjectName)
        .sources(sourceObjects)
        .build();

    log.info("Composing object {} sources [{}].", destObjectName, sourceObjects.stream()
        .map(so -> String.format("bucketName %s objectName %s", so.bucket(), so.object()))
        .collect(Collectors.joining(",")));
    return minioClient.composeObject(composeObjectArgs);
  }

}
