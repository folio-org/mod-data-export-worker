package org.folio.dew.repository;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.config.properties.RemoteFilesStorageProperties;
import org.folio.s3.client.PutObjectAdditionalOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import lombok.extern.log4j.Log4j2;

@Repository
@Log4j2
public class RemoteFilesStorage extends AbstractFilesStorage {

  public static final String CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME = "attachment";
  public static final String CONTENT_DISPOSITION_HEADER_WITH_FILENAME = CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME
      + "; filename=\"%s\"";

  @Autowired
  private LocalFilesStorage localFilesStorage;

  public RemoteFilesStorage(RemoteFilesStorageProperties properties) {
    super(properties);
  }

  public String uploadObject(String object, String filename, String downloadFilename, String contentType,
      boolean isSourceShouldBeDeleted) throws IOException {
    log.info("Uploading object {},filename {},downloadFilename {},contentType {}.", object, filename, downloadFilename,
        contentType);

    byte[] bytes = localFilesStorage.readAllBytes(filename);
    var result = write(object, new ByteArrayInputStream(bytes), bytes.length,
        prepareAdditionalOptions(downloadFilename, contentType));

    if (isSourceShouldBeDeleted) {
      localFilesStorage.remove(filename);
      log.info("Deleted temp file {}.", filename);
    }

    return result;
  }

  public boolean containsFile(String fileName) {
    return this.exists(fileName);
  }

  public String composeObject(String destObject, List<String> sourceObjects, String downloadFilename, String contentType) {
    log.info("Composing object {} from {}", destObject, sourceObjects);
    String result = compose(destObject, sourceObjects, prepareAdditionalOptions(downloadFilename, contentType));

    removeObjects(sourceObjects);

    return result;
  }

  public void removeObjects(List<String> objects) {
    log.info("Deleting objects [{}].", StringUtils.join(objects, ","));
    remove(objects.toArray(s -> new String[s]));
  }

  public String objectToPresignedObjectUrl(String object) {
    String result = getPresignedUrl(object);
    log.info("Created presigned URL {}.", result);
    return result;
  }

  private PutObjectAdditionalOptions prepareAdditionalOptions(String downloadFilename, String contentType) {
    PutObjectAdditionalOptions.PutObjectAdditionalOptionsBuilder builder = PutObjectAdditionalOptions.builder();

    if (StringUtils.isNotBlank(downloadFilename)) {
      builder = builder.contentDisposition(String.format(CONTENT_DISPOSITION_HEADER_WITH_FILENAME, downloadFilename));
    } else {
      builder = builder.contentDisposition(CONTENT_DISPOSITION_HEADER_WITHOUT_FILENAME);
    }
    if (StringUtils.isNotBlank(contentType)) {
      builder = builder.contentType(contentType);
    }

    return builder.build();
  }

}
