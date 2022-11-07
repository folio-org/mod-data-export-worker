package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.domain.dto.EdiFtp;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.FTPObjectStorageRepository;
import org.folio.dew.repository.SFTPObjectStorageRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Service
@Log4j2
public class FTPStorageService {
  private final SFTPObjectStorageRepository sftpObjectStorageRepository;
  private final FTPObjectStorageRepository ftpObjectStorageRepository;
  private static final String SFTP_PROTOCOL = "sftp://";

  public void uploadToFtp(VendorEdiOrdersExportConfig ediExportConfig, byte[] fileByteContent, String filename) throws Exception {
    String username = ediExportConfig.getEdiFtp().getUsername();
    String folder = ediExportConfig.getEdiFtp().getOrderDirectory();
    String password = ediExportConfig.getEdiFtp().getPassword();
    String host = ediExportConfig.getEdiFtp().getServerAddress().replace(SFTP_PROTOCOL, "");
    int port = ediExportConfig.getEdiFtp().getFtpPort();

    if (EdiFtp.FtpFormatEnum.SFTP.equals(ediExportConfig.getEdiFtp().getFtpFormat())) {
      sftpObjectStorageRepository.upload(username, password, host, port, folder, filename, fileByteContent);
    }
    else {
      ftpObjectStorageRepository.upload(host, username, password, filename, fileByteContent);
    }
  }
}
