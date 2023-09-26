package org.folio.dew.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.net.ftp.FTPClient;
import org.folio.dew.config.JacksonConfiguration;
import org.folio.dew.config.properties.FTPProperties;
import org.folio.dew.exceptions.FtpException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.Permissions;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.extern.log4j.Log4j2;

@Log4j2
@SpringBootTest(classes ={JacksonConfiguration.class,
  FTPObjectStorageRepository.class,
  FTPProperties.class,
  FTPClient.class})
class FTPObjectStorageRepositoryTest {

  private static final String ALLOWED_PATH = "/files/upload/";
  private static final String FORBIDDEN_PATH = "/invalid/path/";
  private static final String FILE_NAME = "filename.txt";
  private static final String USERNAME_VALID = "validUser";
  private static final String PASSWORD_VALID = "letMeIn";
  private static final String PASSWORD_INVALID = "don'tLetMeIn";
  private static final String INVALID_URI = "http://localhost";
  private static final byte[] FILE_CONTENT = "Some text".getBytes();

  private static FakeFtpServer fakeFtpServer;

  private static String uri;

  @Autowired
  private FTPObjectStorageRepository repository;
  @Autowired
  private ObjectFactory<FTPClient> ftpClientFactory;
  @Autowired
  private FTPProperties properties;

  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSz", Locale.ENGLISH);

  static {
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @BeforeAll
  public static void setup() {
    fakeFtpServer = new FakeFtpServer();
    fakeFtpServer.setServerControlPort(0); // use any free port

    FileSystem fileSystem = new UnixFakeFileSystem();
    fileSystem.add(new DirectoryEntry(ALLOWED_PATH));
    DirectoryEntry forbidden = new DirectoryEntry(FORBIDDEN_PATH);
    forbidden.setPermissions(Permissions.NONE);
    fileSystem.add(forbidden);
    fakeFtpServer.setFileSystem(fileSystem);

    UserAccount userAccount = new UserAccount(USERNAME_VALID, PASSWORD_VALID, "/");

    fakeFtpServer.addUserAccount(userAccount);

    fakeFtpServer.start();

    uri = "ftp://localhost:" + fakeFtpServer.getServerControlPort() + "/";
    log.info("Mock FTP server running at: " + uri);
  }

  @AfterAll
  public static void teardown() {
    if (fakeFtpServer != null && !fakeFtpServer.isShutdown()) {
      log.info("Shutting down mock FTP server");
      fakeFtpServer.stop();
    }
  }

  @Test
  void testFailedConnect() {
    log.info("=== Test unsuccessful login ===");

    Exception exception = assertThrows(URISyntaxException.class, () -> {
      repository.upload(INVALID_URI, USERNAME_VALID, PASSWORD_VALID, FILE_NAME, FILE_NAME, FILE_CONTENT);
    });

    String expectedMessage = "URI should be valid ftp path";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  void testFailedLogin() {
    log.info("=== Test unsuccessful login ===");
    assertThrows(FtpException.class, () -> repository.upload(uri, USERNAME_VALID, PASSWORD_INVALID, "/", FILE_NAME, FILE_CONTENT));
  }

  @Test
  void testSuccessfulUpload() {
    log.info("=== Test successful upload ===");

    assertDoesNotThrow(() -> repository.upload(uri, USERNAME_VALID, PASSWORD_VALID, ALLOWED_PATH, FILE_NAME, FILE_CONTENT));
    assertTrue(fakeFtpServer.getFileSystem().exists(ALLOWED_PATH + FILE_NAME));
  }

  @Test
  void testFailedUpload() {
    log.info("=== Test unsuccessful upload ===");
    assertThrows(
      FtpException.class,
      () -> repository.upload(uri, USERNAME_VALID, PASSWORD_VALID, "/invalid/path/", FILE_NAME, FILE_CONTENT)
    );
  }
}
