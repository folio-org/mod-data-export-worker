package org.folio.dew.batch.acquisitions.edifact.services;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.folio.dew.domain.dto.EdiFtp;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.FTPObjectStorageRepository;
import org.folio.dew.repository.SFTPObjectStorageRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Service
@Log4j2
public class SaveToFTPStorageService {
  private final SFTPObjectStorageRepository sftpObjectStorageRepository;
  private final FTPObjectStorageRepository ftpObjectStorageRepository;
  private final OrganizationsService organizationsService;

  private static final String SFTP_PROTOCOL = "sftp://";

  @SneakyThrows
  public String uploadToFtp(VendorEdiOrdersExportConfig ediExportConfig, String fileContent) {
    String filename = generateFileName(ediExportConfig);
    uploadToFtp(ediExportConfig, fileContent, filename);
    return filename;
  }

  public void uploadToFtp(VendorEdiOrdersExportConfig ediExportConfig, String fileContent, String filename) throws Exception {
    String username = ediExportConfig.getEdiFtp().getUsername();
    String folder = ediExportConfig.getEdiFtp().getOrderDirectory();
    String password = ediExportConfig.getEdiFtp().getPassword();
    String host = ediExportConfig.getEdiFtp().getServerAddress().replace(SFTP_PROTOCOL, "");
    int port = ediExportConfig.getEdiFtp().getFtpPort();

    if (EdiFtp.FtpFormatEnum.SFTP.equals(ediExportConfig.getEdiFtp().getFtpFormat())) {
      sftpObjectStorageRepository.upload(username, password, host, port, folder, filename, fileContent);
    }
    else {
      ftpObjectStorageRepository.login(host, username,password);
      ftpObjectStorageRepository.upload(filename, fileContent);
    }
  }

  private String generateFileName(VendorEdiOrdersExportConfig ediExportConfig) {
    var orgName = organizationsService.getOrganizationById(ediExportConfig.getVendorId().toString()).get("code").asText();
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    var fileDate = dateFormat.format(new Date());
    // exclude restricted symbols after implementing naming convention feature
    return orgName + "_" + ediExportConfig.getConfigName() + "_" + fileDate + ".edi";
  }
}
