package org.folio.dew.config;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.config.properties.LocalFilesStorageProperties;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.folio.s3.exception.S3ClientException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class LocalS3RepositoryConfig {

  @Bean
  public FolioS3Client localFolioS3Client(LocalFilesStorageProperties localS3Properties) {
    log.debug("remote-files-storage: endpoint {}, region {}, bucket {}, accessKey {}, secretKey {}, subPath{}, awsSdk {}",
      localS3Properties.getEndpoint(), localS3Properties.getRegion(),
      localS3Properties.getBucket(), localS3Properties.getAccessKey(),
      localS3Properties.getSecretKey(), localS3Properties.getSubPath()
      ,localS3Properties.isComposeWithAwsSdk());
    var client = S3ClientFactory.getS3Client(S3ClientProperties.builder()
      .endpoint(localS3Properties.getEndpoint())
      .secretKey(localS3Properties.getSecretKey())
      .accessKey(localS3Properties.getAccessKey())
      .bucket(localS3Properties.getBucket())
      .awsSdk(localS3Properties.isComposeWithAwsSdk())
      .region(localS3Properties.getRegion())
      .subPath(localS3Properties.getSubPath())
      .build());
    try {
      client.createBucketIfNotExists();
    } catch (S3ClientException e) {
      log.error("Error creating bucket: {} during RemoteStorageClient initialization" , localS3Properties.getBucket());
    }
    return client;
  }
}
