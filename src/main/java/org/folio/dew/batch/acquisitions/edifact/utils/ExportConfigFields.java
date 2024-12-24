package org.folio.dew.batch.acquisitions.edifact.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ExportConfigFields {

  INTEGRATION_TYPE("integrationType"),
  TRANSMISSION_METHOD("transmissionMethod"),
  FILE_FORMAT("fileFormat"),
  FTP_PORT("ftpPort"),
  SERVER_ADDRESS("serverAddress"),
  LIB_EDI_TYPE("libEdiType"),
  LIB_EDI_CODE("libEdiCode"),
  VENDOR_EDI_TYPE("vendorEdiType"),
  VENDOR_EDI_CODE("vendorEdiCode"),
  CLAIM_PIECE_IDS("claimPieceIds");

  private final String name;

}
