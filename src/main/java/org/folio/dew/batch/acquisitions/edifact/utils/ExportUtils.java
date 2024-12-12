package org.folio.dew.batch.acquisitions.edifact.utils;

import static org.folio.dew.domain.dto.ReferenceNumberItem.RefNumberTypeEnum.ORDER_REFERENCE_NUMBER;

import java.util.List;
import java.util.Optional;

import org.folio.dew.domain.dto.ReferenceNumberItem;

public class ExportUtils {

  private ExportUtils() { }

  public static ReferenceNumberItem getVendorOrderNumber(List<ReferenceNumberItem> referenceNumberItems) {
    return Optional.ofNullable(referenceNumberItems).orElse(List.of()).stream()
      .filter(r -> r.getRefNumberType() == ORDER_REFERENCE_NUMBER)
      .findFirst()
      .orElse(null);
  }

}
