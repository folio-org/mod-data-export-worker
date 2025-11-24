package org.folio.dew.repository;

import java.nio.charset.StandardCharsets;
import org.folio.dew.config.properties.LocalFilesStorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import io.minio.ObjectWriteArgs;
import lombok.extern.log4j.Log4j2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@SpringBootTest(classes = { LocalFilesStorageProperties.class, LocalFilesStorage.class }, properties = {
    "application.minio-local.compose-with-aws-sdk = true", "application.minio-local.force-path-style = true" })
@EnableConfigurationProperties
class LocalFilesStorageAwsSdkComposingTest extends BaseIntegration {

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

    assertThat(localFilesStorage.write(remoteFilePath, original), is(remoteFilePath));
    assertTrue(localFilesStorage.exists(remoteFilePath));

    assertTrue(Objects.deepEquals(localFilesStorage.readAllBytes(remoteFilePath), original));
    assertTrue(Objects.deepEquals(localFilesStorage.lines(remoteFilePath).toList(), new String(original, StandardCharsets.UTF_8).lines().toList()));

    localFilesStorage.delete(remoteFilePath);
    assertTrue(localFilesStorage.notExists(remoteFilePath));
  }

  private byte[] getRandomBytes(int size) {
    var original = new byte[size];
    ThreadLocalRandom.current()
      .nextBytes(original);
    return original;
  }
}
