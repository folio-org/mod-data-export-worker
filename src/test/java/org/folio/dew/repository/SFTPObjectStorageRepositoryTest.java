package org.folio.dew.repository;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.SystemUtils;
import org.apache.sshd.common.SshException;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@SpringBootTest()
class SFTPObjectStorageRepositoryTest {

  @Autowired
  private SFTPObjectStorageRepository sftpRepository;

  private static final String SFTP_HOST = "localhost";
  private static final String INVALID_HOST = "invalidhost123";
  private static final int PORT = 22;
  private static final String filename = "exported.csv";
  private static final String USERNAME = "sftpuser";
  private static final String PASSWORD = "letmein";
  private static final String PASSWORD_INVALID = "dontLetMeIn";
  private static final String EXPORT_FOLDER_NAME = "exported-files";

  private static final String USER_HOME_DIRECTORY = SystemUtils.getUserHome().getAbsolutePath();

  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSz", Locale.ENGLISH);


  private static final GenericContainer sftp = new GenericContainer(
    new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
        builder
          .from("atmoz/sftp:latest")
          .run("mkdir -p " + USER_HOME_DIRECTORY + File.separator + EXPORT_FOLDER_NAME + "; chmod -R 007 " + USER_HOME_DIRECTORY + File.separator + EXPORT_FOLDER_NAME)
          .build()))
    //.withFileSystemBind(sftpHomeDirectory.getAbsolutePath(), "/home/" + USER + REMOTE_PATH, BindMode.READ_WRITE) //uncomment to mount host directory - not required / recommended
    .withExposedPorts(PORT)
    .withCommand(USERNAME + ":" + PASSWORD + ":1001:::upload");


  static {
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @BeforeAll
  public static void setup() throws IOException {

    sftp.start();
    log.info("SFTP server running at: {}:{}", SFTP_HOST, PORT);
  }

  @AfterAll
  public static void teardown() {
    sftp.stop();
  }

  @Test
  void testSuccessfullyLogin() throws IOException {
    log.info("=== Test successful login ===");

    var sftp = sftpRepository.getSftpClient(USERNAME, PASSWORD, SFTP_HOST, PORT);
    assertNotNull(sftp);
  }

  @Test
  void testFailedConnect() {
    log.info("=== Test unsuccessful login ===");
    Exception exception = assertThrows(SshException.class, () -> {
      sftpRepository.getSftpClient(USERNAME, PASSWORD, INVALID_HOST, PORT);
    });

    String expectedMessage = "Failed (UnresolvedAddressException) to execute";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  void testFailedLogin() {
    log.info("=== Test unsuccessful login ===");
    Exception exception = assertThrows(IOException.class, () -> sftpRepository.getSftpClient(USERNAME, PASSWORD_INVALID, SFTP_HOST, PORT));

    String expectedMessage = "SFTP server authentication failed";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test void testSuccessfulUpload() throws IOException {
    log.info("=== Test successful upload ===");
    Path path = Paths.get("src/test/resources/upload/barcodes.csv");
    String fileContent = Files.lines(path).collect(Collectors.joining("\n"));

    var sshClient = sftpRepository.getSftpClient(USERNAME, PASSWORD, SFTP_HOST, PORT);
    var uploaded = sftpRepository.upload(sshClient, USER_HOME_DIRECTORY, EXPORT_FOLDER_NAME, filename, fileContent);

    assertTrue(uploaded);

    sshClient.close();
  }

  public void startServer() throws IOException {
    var sshd = SshServer.setUpDefaultServer();
    sshd.setHost("localhost");
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
    //Accept all keys for authentication
    sshd.setPublickeyAuthenticator((s, publicKey, serverSession) -> true);
    //Allow username/password authentication using pre-defined credentials
    sshd.setPasswordAuthenticator((username, password, serverSession) ->  USERNAME.equals(username) && USERNAME.equals(password));
    //Setup Virtual File System (VFS)
    //Ensure VFS folder exists

/*    Path dir = Paths.get(getVirtualFileSystemPath());
    Files.createDirectories(dir);
    sshd.setFileSystemFactory(new VirtualFileSystemFactory(dir.toAbsolutePath()));
    //Add SFTP support
    List<NamedFactory<Command>> sftpCommandFactory = new ArrayList<>();
    sftpCommandFactory.add(new SftpSubsystemFactory());
    sshd.setSubsystemFactories(sftpCommandFactory);
*/
    sshd.start();
  }
}
