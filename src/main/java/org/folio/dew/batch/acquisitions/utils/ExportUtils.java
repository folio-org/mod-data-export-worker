package org.folio.dew.batch.acquisitions.utils;

import static org.folio.dew.domain.dto.ReferenceNumberItem.RefNumberTypeEnum.ORDER_REFERENCE_NUMBER;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.ReferenceNumberItem;
import org.folio.dew.domain.dto.VendorDetail;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.FileFormatEnum;

public class ExportUtils {

  private static final String FILE_NAME_FORMAT = "%s_%s_%s.%s";

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

  public static <T> void validateField(String field, T value, Predicate<T> validator, List<String> missingFields) {
    if (!validator.test(value)) {
      missingFields.add(field);
    }
  }

  public static String generateFileName(String vendorName, String configName, FileFormatEnum fileFormat) {
    return FILE_NAME_FORMAT.formatted(vendorName,
      configName,
      new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()),
      fileFormat.getValue().toLowerCase()); // Enum being EDI or CSV
  }

}
