package org.folio.dew.repository;

import io.minio.ObjectWriteArgs;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.config.properties.LocalFilesStorageProperties;
import org.folio.s3.exception.S3ClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.List.of;
import static java.util.stream.Collectors.toList;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@SpringBootTest(classes = {LocalFilesStorageProperties.class, LocalFilesStorage.class})
@EnableConfigurationProperties
class FilesStorageTest extends BaseIntegration {
  private static final String NON_EXISTING_PATH = "non-existing-path";

  @Autowired
  private LocalFilesStorageProperties localFilesStorageProperties;
  @Autowired
  private LocalFilesStorage localFilesStorage;


  @ParameterizedTest
  @ValueSource(ints = {1024, ObjectWriteArgs.MIN_MULTIPART_SIZE + 1 })
  @DisplayName("Create files and read internal objects structure")
  void testWriteRead(int size) throws IOException {
    var subPath = localFilesStorageProperties.getSubPath() + PATH_SEPARATOR;
    byte[] content = getRandomBytes(size);
    var original = of("directory_1/CSV_Data_1.csv", "directory_1/directory_2/CSV_Data_2.csv",
        "directory_1/directory_2/directory_3/CSV_Data_3.csv");
    var expectedS3Pathes = of(subPath + "directory_1/CSV_Data_1.csv", subPath + "directory_1/directory_2/CSV_Data_2.csv",
      subPath + "directory_1/directory_2/directory_3/CSV_Data_3.csv");
    List<String> actual;
    try {
      actual = original.stream()
        .map(p -> {
          try {
            return localFilesStorage.write(p, content);
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        })
        .collect(toList());
    } catch(Exception e) {
      throw new IOException(e);
    }

    assertTrue(Objects.deepEquals(expectedS3Pathes, actual));

    assertTrue(Objects.deepEquals(localFilesStorage.walk(subPath + "directory_1/")
      .collect(toList()),
        of(subPath + "directory_1/CSV_Data_1.csv", subPath + "directory_1/directory_2/CSV_Data_2.csv",
          subPath + "directory_1/directory_2/directory_3/CSV_Data_3.csv")));

    assertTrue(Objects.deepEquals(localFilesStorage.walk(subPath + "directory_1/directory_2/")
      .collect(toList()),
        of(subPath + "directory_1/directory_2/CSV_Data_2.csv", subPath + "directory_1/directory_2/directory_3/CSV_Data_3.csv")));

    original.forEach(p -> assertTrue(localFilesStorage.exists(p)));

    // Clean crated files
    localFilesStorage.delete("directory_1");

    original.forEach(p -> assertFalse(localFilesStorage.exists(p)));
  }


  @ParameterizedTest
  @DisplayName("Create file, update it (append bytes[]), read and delete")
  @ValueSource(ints = { 1024, ObjectWriteArgs.MIN_MULTIPART_SIZE + 1 })
  void testWriteReadPatchDelete(int size) throws IOException {

    byte[] original = getRandomBytes(size);
    var remoteFilePath = "directory_1/directory_2/CSV_Data.csv";
    var expectedS3FilePath = localFilesStorageProperties.getSubPath() + PATH_SEPARATOR + remoteFilePath;

    assertThat(localFilesStorage.write(remoteFilePath, original), is(expectedS3FilePath));
    assertTrue(localFilesStorage.exists(remoteFilePath));

    assertTrue(Objects.deepEquals(localFilesStorage.readAllBytes(remoteFilePath), original));

    var patched = localFilesStorage.readAllBytes(remoteFilePath);
    assertThat(patched.length, is(original.length));

    localFilesStorage.delete(remoteFilePath);
    assertTrue(localFilesStorage.notExists(remoteFilePath));
  }

  @Test
  @DisplayName("Files operations on non-existing file")
  void testNonExistingFileOperations() {
    assertThrows(S3ClientException.class, () -> localFilesStorage.lines(NON_EXISTING_PATH));
    assertThrows(S3ClientException.class, () -> {
      try(var is = localFilesStorage.newInputStream(NON_EXISTING_PATH)){
        log.info("InputStream setup");
      }
    });

    localFilesStorage.delete(NON_EXISTING_PATH);

    assertFalse(localFilesStorage.exists(NON_EXISTING_PATH));
  }

  @Test
  @SneakyThrows
  void testContainsFile() {
    byte[] content = "content".getBytes();
    var path = "directory/data.csv";

    var uploadedPath = localFilesStorage.write(path, content);

    assertEquals("local/directory/data.csv", uploadedPath);
    assertTrue(localFilesStorage.exists(path));
    assertTrue(localFilesStorage.exists(uploadedPath));
  }

  private byte[] getRandomBytes(int size) {
    var original = new byte[size];
    ThreadLocalRandom.current()
      .nextBytes(original);
    return original;
  }
}
