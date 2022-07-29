package org.folio.dew.repository;

import org.folio.dew.config.properties.LocalFilesStorageProperties;
import org.springframework.stereotype.Repository;

import lombok.extern.log4j.Log4j2;

/**
 * Local FS to S3-compatible storage adapter.
 */
@Repository
@Log4j2
public class LocalFilesStorage extends BaseFilesStorage{
  public LocalFilesStorage(LocalFilesStorageProperties properties) {
    super(properties);
  }
}
