package org.folio.dew.batch.acquisitions.edifact.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ExportConfigFields {

  LIB_EDI_TYPE("libEdiType"),
  LIB_EDI_CODE("libEdiCode"),
  VENDOR_EDI_TYPE("vendorEdiType"),
  VENDOR_EDI_CODE("vendorEdiCode"),
  INTEGRATION_TYPE("integrationType"),
  TRANSMISSION_METHOD("transmissionMethod"),
  FILE_FORMAT("fileFormat"),
  CLAIM_PIECE_IDS("claimPieceIds");

  private final String name;

}
