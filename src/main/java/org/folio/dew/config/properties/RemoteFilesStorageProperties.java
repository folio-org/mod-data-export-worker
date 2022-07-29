package org.folio.dew.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("application.minio-remote")
public class RemoteFilesStorageProperties extends MinioClientProperties {
}
