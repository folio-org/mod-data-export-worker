package org.folio.dew.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application properties for object storage client MinIo.
 */
@Component
@ConfigurationProperties("application.minio-local")
public class LocalFilesStorageProperties extends MinioClientProperties {}
