package org.folio.dew.domain.dto.acquisitions.edifact;

import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.PoLine;

public record OrderCsvEntry(PoLine poLine, CompositePurchaseOrder order) {
}
