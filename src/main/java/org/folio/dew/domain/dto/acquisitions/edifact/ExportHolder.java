package org.folio.dew.domain.dto.acquisitions.edifact;

import java.util.List;

import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.Piece;

public record ExportHolder(List<CompositePurchaseOrder> orders, List<Piece> pieces) {

}
