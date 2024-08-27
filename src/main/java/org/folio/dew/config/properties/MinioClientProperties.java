package org.folio.dew.config.properties;

import lombok.Data;

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
   * Folder in s3 bucket.
   */
  private String subPath;

  /**
   * Presigned url expiration time (in seconds).
   */
  private int urlExpirationTimeInSeconds;
}
