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

  private final ObjectFactory<FTPClient> ftpClientFactory;
  private final FTPProperties ftpProperties;
  private static final String FILE_UPLOAD_FAILED = "File upload failed. ";

  public FTPObjectStorageRepository(ObjectFactory<FTPClient> ftpClientFactory, FTPProperties ftpProperties) {
    this.ftpProperties = ftpProperties;
    this.ftpClientFactory = ftpClientFactory;
  }

  private FTPClient login(String ftpUrl, String username, String password) throws URISyntaxException, IOException {
    FTPClient ftpClient = ftpClientFactory.getObject();
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

  public void upload(String ftpUrl, String username, String password, String filename, byte[] fileByteContent) throws Exception {
    FTPClient ftpClient = login(ftpUrl, username, password);
    try (InputStream is = new ByteArrayInputStream(fileByteContent)) {
      ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
      ftpClient.enterLocalPassiveMode();
      changeWorkingDirectory(ftpClient);
      if (ftpClient.storeFile(filename, is)) {
        log.info("File {} uploaded on FTP", filename);
      } else {
        log.warn("File {} NOT uploaded on FTP", filename);
        throw new FtpException(ftpClient.getReplyCode(), getReplyMessage(ftpClient.getReplyCode(), ftpClient.getReplyString()));
      }
    } catch (IOException ioException) {
      log.error("Error uploading file {} with message: {}",filename, ioException.getMessage());
      throw ioException;
    } finally {
      logout(ftpClient);
    }
  }

  private void changeWorkingDirectory(FTPClient ftpClient) throws IOException {
    if (isDirectoryAbsent(ftpClient, ftpProperties.getWorkingDir())) {
      log.info("A directory has been created: " + ftpProperties.getWorkingDir());
      ftpClient.makeDirectory(ftpProperties.getWorkingDir());
    }
    ftpClient.changeWorkingDirectory(ftpProperties.getWorkingDir());
  }

  private boolean isDirectoryAbsent(FTPClient ftpClient, String dirPath) throws IOException {
    ftpClient.changeWorkingDirectory(dirPath);
    int returnCode = ftpClient.getReplyCode();
    return returnCode == 550;
  }

  private boolean isUriValid(String uri) throws URISyntaxException {
    String proto = new URI(uri).getScheme();
    return StringUtils.isEmpty(proto) || proto.equalsIgnoreCase("FTP");
  }

  private void disconnect(FTPClient ftpClient) {
    try {
      ftpClient.disconnect();
    } catch (IOException e) {
      log.error("Error disconnect from FTP", e);
    }
  }

  private static String getReplyMessage(Integer replyCode, String replyMessage) {
    if (replyCode == 550) {
      return FILE_UPLOAD_FAILED + "Please check if user has write permissions";
    }
    return FILE_UPLOAD_FAILED + replyMessage;
  }

}
