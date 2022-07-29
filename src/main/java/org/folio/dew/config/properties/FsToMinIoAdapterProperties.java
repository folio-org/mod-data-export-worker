package org.folio.dew.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Application properties for object storage client MinIo.
 */
@Data
@Component
@ConfigurationProperties("application.minio-fs-adapter")
public class FsToMinIoAdapterProperties {

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
   * The credentials for access to object storage - secretKey.
   */
  private String secretKey;
}
