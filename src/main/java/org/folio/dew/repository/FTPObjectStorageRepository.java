package org.folio.dew.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.folio.dew.config.properties.FTPProperties;
import org.folio.dew.exceptions.FtpException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Repository;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Repository
public class FTPObjectStorageRepository {

  private FTPClient ftpClient;

  private final ObjectFactory<FTPClient> ftpClientFactory;
  private final FTPProperties ftpProperties;

  public FTPObjectStorageRepository(ObjectFactory<FTPClient> ftpClientFactory, FTPProperties ftpProperties) {
    this.ftpProperties = ftpProperties;
    this.ftpClientFactory = ftpClientFactory;
  }

  public boolean login(String ftpUrl, String username, String password) throws URISyntaxException, IOException, FtpException {
    ftpClient = ftpClientFactory.getObject();
    if (!isUriValid(ftpUrl)) {
      throw new URISyntaxException(ftpUrl, "URI should be valid ftp path");
    }

    URI url = new URI(ftpUrl);
    String server = url.getHost();
    int port = url.getPort() > 0 ? url.getPort() : ftpProperties.getDefaultPort();
    ftpClient.connect(server, port);
    log.info("Connected to {}:{}", server, port);
    int reply = ftpClient.getReplyCode();
    if (!FTPReply.isPositiveCompletion(reply)) {
      throw new FtpException(ftpClient.getReplyCode(), ftpClient.getReplyString().trim());
    }

    if (ftpClient.login(username, password)) {
      log.info("Success login to FTP");
    } else {
      log.error("Failed login to FTP");
      disconnect();
      throw new FtpException(ftpClient.getReplyCode(), "Failed login to FTP");
    }

    return true;
  }

  public void logout() {
    try {
      if (ftpClient != null && ftpClient.isConnected()) {
        if (ftpClient.logout()) {
          log.info("Success logout from FTP");
        } else {
          log.error("Failed logout from FTP");
        }
      }
    } catch (Exception e) {
      log.error("Error logging out", e);
    } finally {
      disconnect();
    }
  }

  public boolean upload(String filename, String content) {
    boolean isUpload = false;
    try (InputStream is = new ByteArrayInputStream(content.getBytes())) {
      ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
      ftpClient.enterLocalPassiveMode();
      changeWorkingDirectory();
      if (ftpClient.storeFile(filename, is)) {
        log.debug("File uploaded on FTP");
        isUpload = true;
      } else {
        log.debug("File NOT uploaded on FTP");
        throw new FtpException(ftpClient.getReplyCode(),
          ftpClient.getReplyString().trim());
      }
    } catch (Exception e) {
      log.error("Error uploading", e);
    } finally {
      try {
        ftpClient.logout();
      } catch (IOException e) {
        log.error("Error logout from FTP", e);
      } finally {
        disconnect();
      }
    }
    return isUpload;
  }

  private void changeWorkingDirectory() throws IOException {
    if (isDirectoryAbsent(ftpProperties.getWorkingDir())) {
      log.info("A directory has been created: " + ftpProperties.getWorkingDir());
      ftpClient.makeDirectory(ftpProperties.getWorkingDir());
    }
    ftpClient.changeWorkingDirectory(ftpProperties.getWorkingDir());
  }

  private boolean isDirectoryAbsent(String dirPath) throws IOException {
    ftpClient.changeWorkingDirectory(dirPath);
    int returnCode = ftpClient.getReplyCode();
    return returnCode == 550;
  }

  private boolean isUriValid(String uri) throws URISyntaxException {
    String proto = new URI(uri).getScheme();
    return StringUtils.isEmpty(proto) || proto.equalsIgnoreCase("FTP");
  }

  private void disconnect() {
    try {
      ftpClient.disconnect();
    } catch (IOException e) {
      log.error("Error disconnect from FTP", e);
    }
  }

}
