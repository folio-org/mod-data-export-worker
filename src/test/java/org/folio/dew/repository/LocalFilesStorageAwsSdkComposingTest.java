package org.folio.dew.repository;

import org.apache.commons.lang3.ArrayUtils;
import org.folio.dew.config.properties.LocalFilesStorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import io.minio.ObjectWriteArgs;
import lombok.extern.log4j.Log4j2;

import static java.util.stream.Collectors.toList;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@SpringBootTest(classes = { LocalFilesStorageProperties.class, LocalFilesStorage.class }, properties = {
    "application.minio-local.compose-with-aws-sdk = true", "application.minio-local.force-path-style = true" })
@EnableConfigurationProperties
class LocalFilesStorageAwsSdkComposingTest {

  @Autowired
  private LocalFilesStorageProperties localFilesStorageProperties;
  @Autowired
  private LocalFilesStorage localFilesStorage;

  @ParameterizedTest
  @DisplayName("Create file, update it (append bytes[]), read and delete")
  @ValueSource(ints = { 1024, ObjectWriteArgs.MIN_MULTIPART_SIZE + 1 })
  void testWriteReadPatchDelete(int size) throws IOException {

    byte[] original = getRandomBytes(size);
    var remoteFilePath = "CSV_Data.csv";
    var expectedS3Path = localFilesStorageProperties.getSubPath() + PATH_SEPARATOR + remoteFilePath;

    assertThat(localFilesStorage.write(remoteFilePath, original), is(expectedS3Path));
    assertTrue(localFilesStorage.exists(remoteFilePath));

    assertTrue(Objects.deepEquals(localFilesStorage.readAllBytes(remoteFilePath), original));
    assertTrue(Objects.deepEquals(localFilesStorage.lines(remoteFilePath)
      .collect(toList()), localFilesStorage.readAllLines(remoteFilePath)));

    localFilesStorage.delete(remoteFilePath);
    assertTrue(localFilesStorage.notExists(remoteFilePath));
  }

  @Test
  @DisplayName("Append files with using AWS SDK workaround instead of MinIO client composeObject-method")
  void testAppendFileParts() throws IOException {

    var name = "directory_1/directory_2/CSV_Data.csv";
    byte[] file = getRandomBytes(30000000);
    var size = file.length;

    var first = Arrays.copyOfRange(file, 0, size / 3);
    var second = Arrays.copyOfRange(file, size / 3, 2 * size / 3);
    var third = Arrays.copyOfRange(file, 2 * size / 3, size);

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
