package org.folio.dew.repository;


import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteFilesStorageTest extends BaseBatchTest {

  @Autowired
  private RemoteFilesStorage remoteFilesStorage;

  @Test
  @SneakyThrows
  void testContainsFile() {
    byte[] content = "content".getBytes();
    var path = "directory/data.csv";

    var uploadedPath = remoteFilesStorage.write(path, content);

    assertEquals("remote/directory/data.csv", uploadedPath);
    assertTrue(remoteFilesStorage.containsFile(path));
    assertTrue(remoteFilesStorage.containsFile(uploadedPath));
  }
}
