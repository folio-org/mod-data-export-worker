package org.folio.dew.repository;


import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteFilesStorageTest extends BaseBatchTest {

  @Autowired
  private RemoteFilesStorage remoteFilesStorage;

  @BeforeAll
  static void beforeAll() {
    setUpTenant(NON_CONSORTIUM_TENANT);
  }

  @Test
  @SneakyThrows
  void testContainsFile() {
    byte[] content = "content".getBytes();
    var path = "directory/data.csv";

    var uploadedPath = remoteFilesStorage.write(path, content);

    assertEquals("directory/data.csv", uploadedPath);
    assertTrue(remoteFilesStorage.containsFile(path));
    assertTrue(remoteFilesStorage.containsFile(uploadedPath));
  }
}
