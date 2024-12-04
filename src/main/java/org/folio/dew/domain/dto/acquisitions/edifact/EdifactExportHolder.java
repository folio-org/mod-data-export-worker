package org.folio.dew.domain.dto.acquisitions.edifact;

import java.util.List;

import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.Piece;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EdifactExportHolder {

  private List<CompositePurchaseOrder> orders;
  private List<Piece> pieces;

}
