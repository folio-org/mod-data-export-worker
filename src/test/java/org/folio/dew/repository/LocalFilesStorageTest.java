package org.folio.dew.repository;

import io.minio.ObjectWriteArgs;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.config.properties.LocalFilesStorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.List.of;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Log4j2
@SpringBootTest(classes = {LocalFilesStorageProperties.class, LocalFilesStorage.class})
@EnableConfigurationProperties
class LocalFilesStorageTest {
  private static final String NON_EXISTING_PATH = "non-existing-path";

  @Autowired
  private LocalFilesStorage localFilesStorage;

  @ParameterizedTest
  @ValueSource(ints = {1024, ObjectWriteArgs.MIN_MULTIPART_SIZE + 1 })
  @DisplayName("Create files and read internal objects structure")
  void testWriteRead(int size) throws IOException {
    byte[] content = getRandomBytes(size);
    var original = of("directory_1/CSV_Data_1.csv", "directory_1/directory_2/CSV_Data_2.csv",
        "directory_1/directory_2/directory_3/CSV_Data_3.csv");

    List<String> expected;
    try {
      expected = original.stream()
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

    assertTrue(Objects.deepEquals(original, expected));

    assertTrue(Objects.deepEquals(localFilesStorage.walk("directory_1/")
      .collect(toList()),
        of("directory_1/CSV_Data_1.csv", "directory_1/directory_2/CSV_Data_2.csv",
          "directory_1/directory_2/directory_3/CSV_Data_3.csv")));

    assertTrue(Objects.deepEquals(localFilesStorage.walk("directory_1/directory_2/")
      .collect(toList()),
        of("directory_1/directory_2/CSV_Data_2.csv", "directory_1/directory_2/directory_3/CSV_Data_3.csv")));

    original.forEach(p -> assertTrue(localFilesStorage.exists(p)));

    // Clean crated files
    localFilesStorage.delete("directory_1");

    original.forEach(p -> assertFalse(localFilesStorage.exists(p)));
  }

  @ParameterizedTest
  @ValueSource(ints = {1024, 2048})
  @DisplayName("Buffered writer test")
  void testBufferedWriter(int size) {
    var path = "directory/resource.csv";
    var expected = new String(getRandomBytes(size));
    try(BufferedWriter writer = localFilesStorage.writer(path)) {
      writer.write(expected);
    } catch (IOException e) {
      fail("Writer exception");
    }

    try(InputStream is = localFilesStorage.newInputStream(path)) {
      var actual = new String(is.readAllBytes());
      assertEquals(expected, actual);
    } catch (IOException e) {
      fail("Read resource exception");
    }

    // Clean crated files
    localFilesStorage.delete(path);
  }

  @ParameterizedTest
  @DisplayName("Create file, update it (append bytes[]), read and delete")
  @ValueSource(ints = { 1024, ObjectWriteArgs.MIN_MULTIPART_SIZE + 1 })
  void testWriteReadPatchDelete(int size) throws IOException {

    byte[] original = getRandomBytes(size);
    byte[] patch = getRandomBytes(size);
    var remoteFilePath = "directory_1/directory_2/CSV_Data.csv";

    assertThat(localFilesStorage.write(remoteFilePath, original), is(remoteFilePath));
    assertTrue(localFilesStorage.exists(remoteFilePath));

    assertTrue(Objects.deepEquals(localFilesStorage.readAllBytes(remoteFilePath), original));
    assertTrue(Objects.deepEquals(localFilesStorage.lines(remoteFilePath)
      .collect(toList()), localFilesStorage.readAllLines(remoteFilePath)));

    localFilesStorage.append(remoteFilePath, patch);

    var patched = localFilesStorage.readAllBytes(remoteFilePath);
    assertThat(patched.length, is(original.length + patch.length));

    localFilesStorage.delete(remoteFilePath);
    assertTrue(localFilesStorage.notExists(remoteFilePath));
  }

  @Test
  @DisplayName("Files operations on non-existing file")
  void testNonExistingFileOperations() {
    assertThrows(IOException.class, () -> localFilesStorage.readAllLines(NON_EXISTING_PATH));
    assertThrows(IOException.class, () -> localFilesStorage.lines(NON_EXISTING_PATH));
    assertThrows(IOException.class, () -> {
      try(var is = localFilesStorage.newInputStream(NON_EXISTING_PATH)){
        log.info("InputStream setup");
      }
    });

    localFilesStorage.delete(NON_EXISTING_PATH);

    assertFalse(localFilesStorage.exists(NON_EXISTING_PATH));
  }

  private byte[] getRandomBytes(int size) {
    var original = new byte[size];
    ThreadLocalRandom.current()
      .nextBytes(original);
    return original;
  }
}
