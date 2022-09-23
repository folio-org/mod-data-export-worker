package org.folio.dew.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.spring.integration.ApacheSshdSftpSessionFactory;
import org.folio.dew.batch.acquisitions.edifact.exceptions.EdifactException;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Log4j2
@Repository
@RequiredArgsConstructor
public class SFTPObjectStorageRepository {

  private SshSimpleClient sshClient;
 private final LocalFilesStorage localFilesStorage;
  private static final int LOGIN_TIMEOUT_SECONDS = 5;

  public SftpClient getSftpClient(String username, String password, String host, int port) throws IOException {
    sshClient = new SshSimpleClient(username, password, host, port);
    sshClient.startClient();
    ClientSession clientSession = sshClient.connect();

    AuthFuture auth = clientSession.auth();
    auth.await(LOGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    if (auth.isSuccess()) {
      log.info("authentication successful: {}", auth.isSuccess());
      return createSftpClient(clientSession);
    } else {
      clientSession.close();
      sshClient.stopClient();
      throw new IOException("SFTP server authentication failed");
    }
  }

  private SftpClient createSftpClient(ClientSession session) throws IOException {
    return SftpClientFactory.instance().createSftpClient(session);
  }


  protected ApacheSshdSftpSessionFactory getSshdSessionFactory(String username, String password, String host, int port) throws Exception {
    var ssh = SshClient.setUpDefaultClient();
    ssh.start();

    ApacheSshdSftpSessionFactory factory = new ApacheSshdSftpSessionFactory(false);
    factory.setHost(host);
    factory.setPort(port);
    factory.setUsername(username);
    factory.setPassword(password);
    factory.setSshClient(ssh);
    factory.setConnectTimeout(TimeUnit.SECONDS.toMillis(7L));
    factory.setAuthenticationTimeout(TimeUnit.SECONDS.toMillis(11L));
    factory.afterPropertiesSet();
    return factory;
  }

  public boolean upload(String username, String password, String host, int port, String folder, String filename, String content)
      throws Exception {
    var srcFile = createTempFile(filename, content);
    String folderPath = StringUtils.isEmpty(folder) ? "" : (folder + File.separator);
    String remoteAbsPath = folderPath + filename;

    SessionFactory<SftpClient.DirEntry> sshdFactory;
    try {
      sshdFactory = getSshdSessionFactory(username, password, host, port);
    } catch (Exception e) {
      throw new EdifactException(String.format("Unable to connect to %s:%d", host, port));
    }
    try (InputStream inputStream = localFilesStorage.newInputStream(srcFile); var session = sshdFactory.getSession()) {
      log.info("Start uploading file to SFTP path: {}", remoteAbsPath);

      createRemoteDirectoryIfAbsent(session, folder);
      session.write(inputStream, remoteAbsPath);

      return true;
    } catch (Exception e) {
      log.info("Error uploading the file", e);
      return false;
    } finally {
      localFilesStorage.delete(srcFile);
    }
  }

  private String createTempFile(String filename, String content) throws IOException {

    localFilesStorage.delete(filename);
    localFilesStorage.write(filename, StringUtils.getBytes(content, StandardCharsets.UTF_8));

    return filename;
  }

  public byte[] download(SftpClient sftpClient, String path) {
    byte[] fileBytes = null;
    try {
      InputStream stream = sftpClient.read(path);
      log.info("File found from path: {}", path);
      fileBytes = stream.readAllBytes();
      stream.close();
    } catch (IOException e) {
      log.error(e);
    }
    return fileBytes;
  }

  private void createRemoteDirectoryIfAbsent(Session<SftpClient.DirEntry> session, String folder) throws IOException {
    if (!session.exists(folder)) {
      String[] folders = folder.split("/");
      StringBuilder path = new StringBuilder(folders[0]).append("/");

      for (int i = 0; i < folders.length; i++) {
        if (!session.exists(path.toString())) {
          session.mkdir(path.toString());
        }
        if (i == folders.length - 1) return;
        path.append(folders[i + 1]).append("/");
      }
      log.info("A directory has been created: {}", folder);
    }
  }


  public void logout() {
    if (sshClient.getSshClient().isStarted()) {
      sshClient.stopClient();
    }
  }

}
