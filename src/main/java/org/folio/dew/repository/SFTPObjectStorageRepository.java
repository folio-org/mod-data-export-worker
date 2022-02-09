package org.folio.dew.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystemProvider;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.apache.sshd.sftp.spring.integration.ApacheSshdSftpSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.apache.sshd.sftp.common.SftpHelper.DEFAULT_SUBSTATUS_MESSAGE;

@Log4j2
@Repository
@RequiredArgsConstructor
public class SFTPObjectStorageRepository {

  private final ObjectMapper objectMapper;

  private SshSimpleClient sshClient;
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

/*  protected ApacheSshdSftpSessionFactory getSshdSessionFactory(SshSimpleClient client) throws Exception {
    return getSshdSessionFactory(client,false);
  }*/

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

  public boolean upload(String username, String password, String host, int port, String folder, String filename, String content) throws Exception {

    Path srcFile = Paths.get(filename);
    Files.deleteIfExists(srcFile);

    var srcFilePath = Files.createFile(srcFile);
    FileWriter myWriter = new FileWriter(filename);
    myWriter.write(content);
    myWriter.close();

    String folderPath = StringUtils.isEmpty(folder) ? "" : (folder + File.separator);
    String remoteAbsPath = folderPath + filename;

    //createRemoteDirectoryIfAbsent(sftpClient, folder);
    SessionFactory<SftpClient.DirEntry> sshdFactory = getSshdSessionFactory(username, password, host, port);

    // URI uri = SftpFileSystemProvider.createFileSystemURI(sshClient.getHost(), sshClient.getPort(), sshClient.getUsername(), sshClient.getPassword());
    var session = sshdFactory.getSession();
    try (InputStream inputStream = Files.newInputStream(srcFile)) {
      session.write(inputStream, remoteAbsPath);
      session.close();
      return true;
    } catch (Exception e){
      session.close();
      return false;
    }

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

  private void createRemoteDirectoryIfAbsent(SftpClient sftpClient, String folder) throws IOException {
    if (isDirectoryAbsent(sftpClient, folder)) {
      String[] folders = folder.split("/");
      StringBuilder path = new StringBuilder(folders[0]).append("/");

      for (int i = 0; i < folders.length; i++) {
        if (isDirectoryAbsent(sftpClient, path.toString())) {
          sftpClient.mkdir(path.toString());
        }
        if (i == folders.length - 1) return;
        path.append(folders[i + 1]).append("/");
      }
      log.info("A directory has been created: {}", folder);
    }
  }

  private boolean isDirectoryAbsent(SftpClient sftpClient, String folder) throws IOException {
    try {
      sftpClient.open(folder, SftpClient.OpenMode.Read);
    } catch (SftpException sftpException) {
      if (DEFAULT_SUBSTATUS_MESSAGE.get(SftpConstants.SSH_FX_NO_SUCH_FILE).contains(sftpException.getMessage())) {
        return true;
      } else throw sftpException;
    }
    return false;
  }

  public void logout() {
    if (sshClient.getSshClient().isStarted()) {
      sshClient.stopClient();
    }
  }

}
