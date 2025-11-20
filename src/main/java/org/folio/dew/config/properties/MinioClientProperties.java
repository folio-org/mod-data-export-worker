package org.folio.dew.config.properties;

import lombok.Data;
import org.folio.s3.client.S3ClientProperties;

@Data
public class MinioClientProperties {

  /**
   * URL to object storage.
   */
  private String endpoint;

  /**
   * The region to configure the url.
   */
  private String region;

  /**
   * The object storage bucket.
   */
  private String bucket;

  /**
   * The credentials for access to object storage - accessKey.
   */
  private String accessKey;

  /**
   *  The credentials for access to object storage - secretKey.
   */
  private String secretKey;

  /**
   * Key that enables files merging in storage with using AWS SDK capabilities.
   */
  private boolean composeWithAwsSdk;

  /**
   * Forces the AWS SDK client to use path-style, not virtual-host-style, addressing for buckets.
   * Needed for LocalFilesStorageAwsSdkComposingTest.
   */
  private boolean forcePathStyle;

  /**
   * Path in s3 bucket.
   */
  private String subPath;

  /**
   * Presigned url expiration time (in seconds).
   */
  private int urlExpirationTimeInSeconds;

  public S3ClientProperties toS3ClientProperties() {
    return S3ClientProperties
      .builder()
      .endpoint(endpoint)
      .region(region)
      .bucket(bucket)
      .accessKey(accessKey)
      .secretKey(secretKey)
      .awsSdk(composeWithAwsSdk)
      .forcePathStyle(forcePathStyle)
      .subPath(subPath)
      .build();
  }
}
