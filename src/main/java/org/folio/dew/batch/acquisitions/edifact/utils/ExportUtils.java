package org.folio.dew.batch.acquisitions.edifact.utils;

import static org.folio.dew.domain.dto.ReferenceNumberItem.RefNumberTypeEnum.ORDER_REFERENCE_NUMBER;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.ReferenceNumberItem;
import org.folio.dew.domain.dto.VendorDetail;

public class ExportUtils {

  private ExportUtils() { }

  public static List<ReferenceNumberItem> getVendorReferenceNumbers(CompositePoLine poLine) {
    return Optional.ofNullable(poLine.getVendorDetail())
      .map(VendorDetail::getReferenceNumbers)
      .orElse(new ArrayList<>());
  }

  public static ReferenceNumberItem getVendorOrderNumber(List<ReferenceNumberItem> referenceNumberItems) {
    return Optional.ofNullable(referenceNumberItems).orElse(List.of()).stream()
      .filter(r -> r.getRefNumberType() == ORDER_REFERENCE_NUMBER)
      .findFirst()
      .orElse(null);
  }

  public static String getVendorOrderNumber(CompositePoLine poLine) {
    return Optional.ofNullable(getVendorOrderNumber(getVendorReferenceNumbers(poLine)))
      .map(ReferenceNumberItem::getRefNumber)
      .orElse(null);
  }

  public static String getVendorAccountNumber(CompositePoLine poLine) {
    return Optional.ofNullable(poLine.getVendorDetail())
      .map(VendorDetail::getVendorAccount)
      .orElse(null);
  }

}
