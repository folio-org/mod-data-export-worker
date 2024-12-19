package org.folio.dew.batch.acquisitions.edifact.mapper;

import java.util.List;

import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;

import io.xlate.edi.stream.EDIStreamException;

public interface ExportResourceMapper {

  String convertForExport(List<CompositePurchaseOrder> compPOs, List<Piece> pieces,
                          VendorEdiOrdersExportConfig ediExportConfig, String jobName) throws EDIStreamException;

}
