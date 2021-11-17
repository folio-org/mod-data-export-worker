package org.folio.dew.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.net.ftp.FTPClient;
import org.folio.dew.config.FTPConfig;
import org.folio.dew.config.properties.FTPProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import lombok.extern.log4j.Log4j2;

@Log4j2
class FTPObjectStorageRepositoryTest {

  private static FakeFtpServer fakeFtpServer;

  private static final String user_home_dir = "/files";
  private static final String filename = "filename.txt";
  private static final String username_valid = "validUser";
  private static final String password_valid = "letMeIn";
  private static final String password_invalid = "don'tLetMeIn";
  private static final String invalid_uri = "http://localhost";

  private static String uri;
  private static FTPClient client;
  private static FTPProperties properties;

  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSz", Locale.ENGLISH);

  static {
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @BeforeAll
  public static void setup() {
    fakeFtpServer = new FakeFtpServer();
    fakeFtpServer.setServerControlPort(0); // use any free port

    FileSystem fileSystem = new UnixFakeFileSystem();
    fileSystem.add(new DirectoryEntry(user_home_dir));
    fakeFtpServer.setFileSystem(fileSystem);

    UserAccount userAccount = new UserAccount(username_valid, password_valid, user_home_dir);

    fakeFtpServer.addUserAccount(userAccount);

    fakeFtpServer.start();

    uri = "ftp://localhost:" + fakeFtpServer.getServerControlPort() + "/";
    log.info("Mock FTP server running at: " + uri);

    properties = new FTPProperties();
    properties.setDefaultPort(21);
    properties.setDefaultTimeout(30000);
    properties.setControlKeepAliveTimeout(30);
    properties.setBufferSize(1024 * 1024);

    client = new FTPClient();
    client = new FTPClient();
    client.setDefaultTimeout(properties.getDefaultTimeout());
    client.setControlKeepAliveTimeout(properties.getControlKeepAliveTimeout());
    client.setBufferSize(properties.getBufferSize());
    client.setPassiveNatWorkaroundStrategy(new FTPConfig.DefaultServerResolver(client));
  }

  @AfterAll
  public static void teardown() {
    if (fakeFtpServer != null && !fakeFtpServer.isShutdown()) {
      log.info("Shutting down mock FTP server");
      fakeFtpServer.stop();
    }
  }

  @Test
  void testSuccessfulLogin() throws URISyntaxException {
    log.info("=== Test successful login ===");
    FTPObjectStorageRepository ftpRepository = new FTPObjectStorageRepository(client, properties);

    assertTrue(ftpRepository.login(uri, username_valid, password_valid));
    ftpRepository.logout();
  }

  @Test
  void testFailedConnect() {
    log.info("=== Test unsuccessful login ===");
    FTPObjectStorageRepository ftpRepository = new FTPObjectStorageRepository(client, properties);

    Exception exception = assertThrows(URISyntaxException.class, () -> {
      ftpRepository.login(invalid_uri, username_valid, password_valid);
    });

    String expectedMessage = "URI should be valid ftp path";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  void testFailedLogin() throws URISyntaxException {
    log.info("=== Test unsuccessful login ===");
    FTPObjectStorageRepository ftpRepository = new FTPObjectStorageRepository(client, properties);

    assertFalse(ftpRepository.login(uri, username_valid, password_invalid));
    ftpRepository.logout();
  }

  @Test
  void testSuccessfulUpload() throws URISyntaxException {
    log.info("=== Test successful upload ===");
    FTPObjectStorageRepository ftpRepository = new FTPObjectStorageRepository(client, properties);

    assertTrue(ftpRepository.login(uri, username_valid, password_valid));
    assertTrue(ftpRepository.upload(filename, "Some text"));
    assertTrue(fakeFtpServer.getFileSystem()
      .exists(user_home_dir + "/" + filename));
    ftpRepository.logout();
  }

  @Test
  void testFailedUpload() throws URISyntaxException {
    log.info("=== Test unsuccessful upload ===");
    FTPObjectStorageRepository ftpRepository = new FTPObjectStorageRepository(client, properties);

    assertTrue(ftpRepository.login(uri, username_valid, password_valid));
    assertFalse(ftpRepository.upload("/invalid/path/" + filename, "Some text"));
    ftpRepository.logout();
  }
}
