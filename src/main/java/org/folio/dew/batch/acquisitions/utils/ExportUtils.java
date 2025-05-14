package org.folio.dew.batch.acquisitions.utils;

import static org.folio.dew.batch.acquisitions.utils.ExportConfigFields.FTP_PORT;
import static org.folio.dew.batch.acquisitions.utils.ExportConfigFields.SERVER_ADDRESS;
import static org.folio.dew.domain.dto.ReferenceNumberItem.RefNumberTypeEnum.ORDER_REFERENCE_NUMBER;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.ReferenceNumberItem;
import org.folio.dew.domain.dto.VendorDetail;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.FileFormatEnum;

public class ExportUtils {

  private static final String FILE_NAME_FORMAT = "%s.%s_%s_%s.%s";

  private ExportUtils() { }

  public static List<ReferenceNumberItem> getVendorReferenceNumbers(PoLine poLine) {
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

  public static String getVendorOrderNumber(PoLine poLine) {
    return Optional.ofNullable(getVendorOrderNumber(getVendorReferenceNumbers(poLine)))
      .map(ReferenceNumberItem::getRefNumber)
      .orElse(null);
  }

  public static String getVendorAccountNumber(PoLine poLine) {
    return Optional.ofNullable(poLine.getVendorDetail())
      .map(VendorDetail::getVendorAccount)
      .orElse(null);
  }

  public static String getFormattedDate(Date date) {
    return Optional.ofNullable(date).map(new SimpleDateFormat("yyyy-MM-dd")::format).orElse("");
  }

  public static <T> void validateField(String field, T value, Predicate<T> validator, List<String> missingFields) {
    if (!validator.test(value)) {
      missingFields.add(field);
    }
  }

  public static void validateFtpFields(VendorEdiOrdersExportConfig ediExportConfig, List<String> missingFields) {
    if (ediExportConfig.getIntegrationType() == ORDERING || ediExportConfig.getTransmissionMethod() == FTP) {
      var ftpConfig = ediExportConfig.getEdiFtp();
      validateField(FTP_PORT.getName(), ftpConfig.getFtpPort(), Objects::nonNull, missingFields);
      validateField(SERVER_ADDRESS.getName(), ftpConfig.getServerAddress(), StringUtils::isNotEmpty, missingFields);
    }
  }

  public static String generateFileName(String vendorName, String configName, VendorEdiOrdersExportConfig.IntegrationTypeEnum integrationType, FileFormatEnum fileFormat) {
    var filePrefix = getFilePrefix(integrationType, fileFormat);
    var timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    return FILE_NAME_FORMAT.formatted(filePrefix, vendorName, configName, timestamp, fileFormat.getValue().toLowerCase());
  }

  public static String getFilePrefix(VendorEdiOrdersExportConfig.IntegrationTypeEnum integrationType, FileFormatEnum fileFormat) {
    if (integrationType == CLAIMING) {
      return fileFormat == FileFormatEnum.EDI ? "edi_claims" : "csv_claims";
    }
    return "edi_orders";
  }

  public static ExportType convertIntegrationTypeToExportType(VendorEdiOrdersExportConfig.IntegrationTypeEnum integrationType) {
		return switch (integrationType) {
			case ORDERING -> ExportType.EDIFACT_ORDERS_EXPORT;
			case CLAIMING -> ExportType.CLAIMS;
		};
  }

}
