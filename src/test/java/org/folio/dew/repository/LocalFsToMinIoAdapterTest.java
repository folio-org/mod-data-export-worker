package org.folio.dew.repository;

import static java.util.List.of;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.minio.ObjectWriteArgs;
import lombok.extern.log4j.Log4j2;

@Log4j2
@SpringBootTest
public class LocalFsToMinIoAdapterTest {
  private static final String NON_EXISTING_PATH = "non-existing-path";
  @Autowired
  private LocalFsToMinIoAdapter adapter;

  @Test
  void testWriteRead() throws IOException {
    log.info("===== Test create files and read internal objects structure =====");
    byte[] content = getRandomBytes(10);
    var original = of("directory_1/CSV_Data_1.csv", "directory_1/directory_2/CSV_Data_2.csv",
        "directory_1/directory_2/directory_3/CSV_Data_3.csv");

    List<String> expected;
    try {
      expected = original.stream()
        .map(p -> {
          try {
            return adapter.write(p, content);
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        })
        .collect(toList());
    } catch(Exception e) {
      throw new IOException(e);
    }

    assertTrue(Objects.deepEquals(original, expected));

    assertTrue(Objects.deepEquals(adapter.list("directory_1/")
      .collect(toList()), of("directory_1/CSV_Data_1.csv", "directory_1/directory_2/")));

    assertTrue(Objects.deepEquals(adapter.list("directory_1/directory_2/")
      .collect(toList()), of("directory_1/directory_2/CSV_Data_2.csv", "directory_1/directory_2/directory_3/")));

    assertTrue(Objects.deepEquals(adapter.walk("directory_1/")
      .collect(toList()),
        of("directory_1/CSV_Data_1.csv", "directory_1/directory_2/CSV_Data_2.csv",
            "directory_1/directory_2/directory_3/CSV_Data_3.csv")));

    assertTrue(Objects.deepEquals(adapter.walk("directory_1/directory_2/")
      .collect(toList()),
        of("directory_1/directory_2/CSV_Data_2.csv", "directory_1/directory_2/directory_3/CSV_Data_3.csv")));
  }

  @ParameterizedTest
  @ValueSource(ints = { 1024, ObjectWriteArgs.MIN_MULTIPART_SIZE + 1 })
  void testWriteReadPatchDelete(int size) throws IOException {

    log.info("===== Test create file, update it (append bytes[]), read and delete =====");

    byte[] original = getRandomBytes(size);
    byte[] patch = getRandomBytes(size);
    var remoteFilePath = "directory_1/directory_2/CSV_Data.csv";

    assertThat(adapter.write(remoteFilePath, original), is(remoteFilePath));
    assertTrue(adapter.exists(remoteFilePath));

    assertTrue(Objects.deepEquals(adapter.readAllBytes(remoteFilePath), original));
    assertTrue(Objects.deepEquals(adapter.lines(remoteFilePath)
      .collect(toList()), adapter.readAllLines(remoteFilePath)));

    adapter.append(remoteFilePath, patch);

    var patched = adapter.readAllBytes(remoteFilePath);
    assertThat(patched.length, is(original.length + patch.length));

    adapter.delete(remoteFilePath);
    assertTrue(adapter.notExists(remoteFilePath));
  }

  @Test
  void testNonExistingFileOperations() throws IOException {
    log.info("===== Test make operations on non-existing file =====");
    assertThrows(IOException.class, () -> adapter.readAllLines(NON_EXISTING_PATH));
    assertThrows(IOException.class, () -> adapter.readAllLines(NON_EXISTING_PATH));
    assertThrows(IOException.class, () -> adapter.lines(NON_EXISTING_PATH));
    assertThrows(IOException.class, () -> {
      try(var is = adapter.newInputStream(NON_EXISTING_PATH)){
        log.info("InputStream setup");
      }
    });

    adapter.delete(NON_EXISTING_PATH);

    assertFalse(adapter.exists(NON_EXISTING_PATH));

    assertThrows(IOException.class, () -> adapter.append(NON_EXISTING_PATH, new byte[1]));
  }

  private byte[] getRandomBytes(int size) {
    var original = new byte[size];
    ThreadLocalRandom.current()
      .nextBytes(original);
    return original;
  }
}
