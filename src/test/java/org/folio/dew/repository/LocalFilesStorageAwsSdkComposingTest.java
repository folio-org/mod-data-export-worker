package org.folio.dew.repository;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.folio.dew.config.properties.LocalFilesStorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@SpringBootTest(classes = {LocalFilesStorageProperties.class, LocalFilesStorage.class},
  properties= {"application.minio-local.compose-with-aws-sdk = true"})
@EnableConfigurationProperties
class LocalFilesStorageAwsSdkComposingTest {

  @Autowired
  private LocalFilesStorage localFilesStorage;

  @Test
  @DisplayName("Append files with using AWS SDK workaround instead of MinIO client composeObject-method")
  void testAppendFileParts() throws IOException {

    var name = "directory_1/directory_2/CSV_Data.csv";
    byte[] file = getRandomBytes(30000000);
    var size = file.length;

    var first = Arrays.copyOfRange(file, 0, size/3);
    var second = Arrays.copyOfRange(file, size/3, 2 * size/3);
    var third = Arrays.copyOfRange(file, 2 * size/3, size);

    var expected = ArrayUtils.addAll(ArrayUtils.addAll(first, second), third);

    assertTrue(Objects.deepEquals(file, expected));

    localFilesStorage.append(name, first);
    localFilesStorage.append(name, second);
    localFilesStorage.append(name, third);

    var result = localFilesStorage.readAllBytes(name);

    assertTrue(Objects.deepEquals(file, result));

  }


  private byte[] getRandomBytes(int size) {
    var original = new byte[size];
    ThreadLocalRandom.current()
      .nextBytes(original);
    return original;
  }
}
