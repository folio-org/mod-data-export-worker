package org.folio.dew.repository;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static org.apache.sshd.sftp.common.SftpHelper.DEFAULT_SUBSTATUS_MESSAGE;

@Log4j2
@Repository
public class SFTPObjectStorageRepository {
  private static final int LOGIN_TIMEOUT_SECONDS = 5;

  private static final EnumSet<SftpClient.OpenMode> DEFAULT_SFTP_MODES = EnumSet.of(SftpClient.OpenMode.Write, SftpClient.OpenMode.Create);

  public SftpClient getSftpClient(String username, String password, String host, int port) throws IOException {
    var sshClient = new SshSimpleClient(username, password, host, port);
    sshClient.startClient();
    ClientSession clientSession = sshClient.connect();

    var auth = clientSession.auth();
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


  public boolean upload(SftpClient sftpClient, String userHomeDirectory, String folder, String filename, String content) throws IOException {
    var folderPath = StringUtils.isEmpty(folder) ? "" : (File.separator + folder + File.separator);
    String fileAbsPath = userHomeDirectory + folderPath + filename;

    createRemoteDirectoryIfAbsent(sftpClient, userHomeDirectory, folder);

    try (sftpClient; SftpClient.CloseableHandle handle = sftpClient.open(fileAbsPath, DEFAULT_SFTP_MODES)) {
      sftpClient.write(handle, 0, content.getBytes(StandardCharsets.UTF_8), 0, content.length());
      log.info("successfully uploaded to SFTP: {}", fileAbsPath);
      return true;
    } catch (IOException e) {
      log.error(e);
    }
    return false;
  }

  private void createRemoteDirectoryIfAbsent(SftpClient sftpClient, String remoteDirAbsPath, String folder) throws IOException {
    if (isDirectoryAbsent(sftpClient, remoteDirAbsPath, folder)) {
      sftpClient.mkdir(folder);
      log.info("A directory has been created: {}", folder);
    }
  }


  private boolean isDirectoryAbsent(SftpClient sftpClient, String userHomeDir, String folder) throws IOException {
    try {
      sftpClient.open(userHomeDir + File.separator + folder, SftpClient.OpenMode.Read);
    } catch (SftpException sftpException) {
      if (DEFAULT_SUBSTATUS_MESSAGE.get(SftpConstants.SSH_FX_NO_SUCH_FILE).contains(sftpException.getMessage())) {
        return true;
      } else throw sftpException;
    }
    return false;
  }

  public static String resolveRelativeRemotePath(Path root, Path file) {
    Path relPath = root.relativize(file);
    return relPath.toString().replace(File.separatorChar, '/');
  }

}
