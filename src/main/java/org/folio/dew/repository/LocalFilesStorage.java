package org.folio.dew.repository;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.config.properties.LocalFilesStorageProperties;
import org.springframework.stereotype.Repository;

/**
 * Local FS to S3-compatible storage adapter.
 */
@Repository
@Log4j2
public class LocalFilesStorage extends AbstractFilesStorage{
  public LocalFilesStorage(LocalFilesStorageProperties properties) {
    super(properties);
  }
}
