package org.folio.dew.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.spring.integration.ApacheSshdSftpSessionFactory;
import org.folio.dew.batch.acquisitions.exceptions.EdifactException;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Log4j2
@Repository
@RequiredArgsConstructor
public class SFTPObjectStorageRepository {

  private SshSimpleClient sshClient;
  private static final int LOGIN_TIMEOUT_SECONDS = 30;

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
    factory.setConnectTimeout(TimeUnit.SECONDS.toMillis(30L));
    factory.setAuthenticationTimeout(TimeUnit.SECONDS.toMillis(30L));
    factory.afterPropertiesSet();
    return factory;
  }

  public boolean upload(String username, String password, String host, int port, String folder, String filename, byte[] content)
      throws Exception {
    String folderPath = StringUtils.isEmpty(folder) ? "" : (folder + File.separator);
    String remoteAbsPath = folderPath + filename;

    SessionFactory<SftpClient.DirEntry> sshdFactory;
    try {
      sshdFactory = getSshdSessionFactory(username, password, host, port);
    } catch (Exception e) {
      log.error("Error connecting to {}:{}", host, port, e);
      throw new EdifactException(String.format("Unable to connect to %s:%d", host, port));
    }
    try (InputStream inputStream = new ByteArrayInputStream(content); var session = sshdFactory.getSession()) {
      log.info("Start uploading file to SFTP path: {}", remoteAbsPath);

      createRemoteDirectoryIfAbsent(session, folder);
      session.write(inputStream, remoteAbsPath);

      return true;
    } catch (Exception e) {
      log.error("Error uploading to SFTP path: {}", remoteAbsPath, e);
      throw new EdifactException(String.format("Unable to upload to sftp %s:%d, folder: %s. %s", host, port, folder, e.getMessage()));
    }
  }

  public byte[] download(SftpClient sftpClient, String path) {
    try (InputStream stream = sftpClient.read(path)) {
      log.info("File found to path: {}", path);
      return stream.readAllBytes();
    } catch (Exception e) {
      log.error("Error downloading from SFTP path: {}", path, e);
      return null;
    }
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
