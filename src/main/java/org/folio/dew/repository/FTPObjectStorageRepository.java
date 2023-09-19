package org.folio.dew.repository;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.folio.dew.config.properties.FTPProperties;
import org.folio.dew.exceptions.FtpException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

@Log4j2
@Repository
public class FTPObjectStorageRepository {

  public static final String ERROR_CAN_NOT_CHANGE_WORKING_DIR = "Can not change working dir. ";
  public static final String ERROR_FILE_UPLOAD_FAILED = "File upload failed. ";

  private final FTPProperties ftpProperties;
  private final ObjectFactory<FTPClient> ftpClientFactory;

  public FTPObjectStorageRepository(ObjectFactory<FTPClient> ftpClientFactory, FTPProperties ftpProperties) {
    this.ftpProperties = ftpProperties;
    this.ftpClientFactory = ftpClientFactory;
  }

  @SneakyThrows
  private FTPClient login(String ftpUrl, String username, String password) {
    URI url = new URI(ftpUrl);
    String scheme = url.getScheme();
    if (StringUtils.isNotEmpty(scheme) && !scheme.equalsIgnoreCase("FTP")) {
      throw new URISyntaxException(ftpUrl, "URI should be valid ftp path");
    }

    FTPClient ftpClient = ftpClientFactory.getObject();
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
      disconnect(ftpClient);
      throw new FtpException(ftpClient.getReplyCode(), "Failed login to FTP");
    }

    return ftpClient;
  }

  private void logout(FTPClient ftpClient) {
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
      if (ftpClient != null) {
        disconnect(ftpClient);
      }
    }
  }

  public void upload(String ftpUrl,
                     String username,
                     String password,
                     String path,
                     String filename,
                     byte[] fileByteContent) throws Exception {

    String remoteAbsPath = path + File.separator + filename;

    FTPClient ftpClient = login(ftpUrl, username, password);
    try (InputStream is = new ByteArrayInputStream(fileByteContent)) {
      ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
      ftpClient.enterLocalPassiveMode();

      changeWorkingDirectory(ftpClient, path);
      if (!ftpClient.storeFile(filename, is)) {
        throw getFtpException(ftpClient, ERROR_FILE_UPLOAD_FAILED);
      }
      log.info("File uploaded to ftp path: {}", remoteAbsPath);

    } catch (Exception e) {
      log.error("Error uploading to ftp path: {}", remoteAbsPath, e);
      throw e;
    } finally {
      logout(ftpClient);
    }
  }

  @SneakyThrows
  private void changeWorkingDirectory(FTPClient ftpClient, String path) {
    for (String dir : path.split(Pattern.quote(File.separator))) {
      if (dir.isEmpty()) {
        continue;
      }
      if (ftpClient.makeDirectory(dir)) {
        log.info("A directory has been created: " + dir);
      }
      if (!ftpClient.changeWorkingDirectory(dir)) {
        throw getFtpException(ftpClient, ERROR_CAN_NOT_CHANGE_WORKING_DIR);
      }
    }
  }

  private void disconnect(FTPClient ftpClient) {
    try {
      ftpClient.disconnect();
    } catch (Exception e) {
      log.error("Error disconnecting from FTP", e);
    }
  }

  private FtpException getFtpException(FTPClient ftpClient, String errorPrefix) {
    return new FtpException(ftpClient.getReplyCode(), errorPrefix + ftpClient.getReplyString());
  }

}
