package org.folio.dew.repository;

import lombok.extern.log4j.Log4j2;
import org.apache.sshd.common.SshException;
import org.apache.sshd.sftp.client.SftpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@SpringBootTest()
class SFTPObjectStorageRepositoryTest {

  @Autowired
  private SFTPObjectStorageRepository sftpRepository;

  private static final int PORT = 22;
  private static final String INVALID_HOST = "invalidhost123";
  private static final String FILENAME = "exported.edi";
  private static final String USERNAME = "user";
  private static final String PASSWORD = "password";
  private static final String PASSWORD_INVALID = "dontLetMeIn";
  private static final String EXPORT_FOLDER_NAME = "upload";

  private static String SFTP_HOST;
  private static Integer MAPPED_PORT;

  @Container
  public static final GenericContainer sftp = new GenericContainer(
    new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
        builder
          .from("atmoz/sftp:latest")
          .run("mkdir -p " + File.separator + EXPORT_FOLDER_NAME + "; chmod -R 777 " + File.separator + EXPORT_FOLDER_NAME)
          .build()))
    .withExposedPorts(PORT)
    .withCommand(USERNAME + ":" + PASSWORD + ":::upload");


  @BeforeAll
  public static void staticSetup() {
    MAPPED_PORT = sftp.getMappedPort(PORT);
    SFTP_HOST = sftp.getHost();
  }

  @Test
  void testSuccessfullyLogin() throws IOException {
    log.info("=== Test successful login ===");
    SftpClient sftp = sftpRepository.getSftpClient(USERNAME, PASSWORD, SFTP_HOST, MAPPED_PORT);

    assertNotNull(sftp);

    sftpRepository.logout();
  }

  @Test
  void testFailedConnect() {
    log.info("=== Test unsuccessful login ===");
    Exception exception = assertThrows(SshException.class, () -> {
      sftpRepository.getSftpClient(USERNAME, PASSWORD, INVALID_HOST, MAPPED_PORT);
    });

    String expectedMessage = "Failed (UnresolvedAddressException) to execute";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  void testFailedLogin() {
    log.info("=== Test unsuccessful login ===");
    Exception exception = assertThrows(IOException.class, () -> sftpRepository.getSftpClient(USERNAME, PASSWORD_INVALID, SFTP_HOST, MAPPED_PORT));

    String expectedMessage = "SFTP server authentication failed";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  void testSuccessfulUpload() throws IOException {
    log.info("=== Test successful upload ===");
    String content = "Some string with content";
    SftpClient sftpClient = sftpRepository.getSftpClient(USERNAME, PASSWORD, SFTP_HOST, MAPPED_PORT);
    boolean uploaded = sftpRepository.upload(sftpClient, EXPORT_FOLDER_NAME, FILENAME, content.getBytes(StandardCharsets.UTF_8));

    assertTrue(uploaded);

    sftpClient.close();
    sftpRepository.logout();
  }

  @Test
  void testSuccessfulUploadForLongPath() throws IOException {
    log.info("=== Test successful upload for long path ===");
    String content = "Some string with content";
    SftpClient sftpClient = sftpRepository.getSftpClient(USERNAME, PASSWORD, SFTP_HOST, MAPPED_PORT);
    boolean uploaded = sftpRepository.upload(sftpClient, EXPORT_FOLDER_NAME + "/test/long/path/creation", FILENAME, content.getBytes(StandardCharsets.UTF_8));

    assertTrue(uploaded);

    sftpClient.close();
    sftpRepository.logout();
  }

  @Test
  void testSuccessfulDownload() throws IOException {
    log.info("=== Test successful download ===");
    String content = "Some string with content for download";
    String path = EXPORT_FOLDER_NAME + "/test/download";
    SftpClient sftpClient = sftpRepository.getSftpClient(USERNAME, PASSWORD, SFTP_HOST, MAPPED_PORT);
    boolean uploaded = sftpRepository.upload(sftpClient, path, FILENAME, content.getBytes(StandardCharsets.UTF_8));
    byte[] fileBytes = sftpRepository.download(sftpClient, path + "/" + FILENAME);

    assertTrue(uploaded);
    assertNotNull(fileBytes);

    sftpClient.close();
    sftpRepository.logout();
  }
}
