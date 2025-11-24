package org.folio.dew.repository;

import java.io.IOException;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.config.properties.RemoteFilesStorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


@Repository
@Log4j2
public class RemoteFilesStorage extends BaseFilesStorage {

  @Autowired
  private LocalFilesStorage localFilesStorage;

  public RemoteFilesStorage(RemoteFilesStorageProperties properties) {
    super(properties);
  }

  public String uploadObject(String object, String filename, String downloadFilename, String contentType, boolean isSourceShouldBeDeleted)
      throws IOException {
    log.info("Uploading object {},filename {},downloadFilename {},contentType {}.", object, filename, downloadFilename,
        contentType);

    var result = write(object, localFilesStorage.readAllBytes(filename), prepareHeaders(downloadFilename, contentType));

    if (isSourceShouldBeDeleted) {
      localFilesStorage.delete(filename);
      log.info("Deleted temp file {}.", filename);
    }

    return result;
  }
}
