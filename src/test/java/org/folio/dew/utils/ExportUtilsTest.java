package org.folio.dew.utils;

import org.folio.dew.CopilotGenerated;
import org.folio.dew.batch.acquisitions.utils.ExportUtils;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.ReferenceNumberItem;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@CopilotGenerated
public class ExportUtilsTest {
  @Test
  void getVendorReferenceNumbersReturnsEmptyListWhenVendorDetailIsNull() {
    PoLine poLine = new PoLine();
    assertThat(ExportUtils.getVendorReferenceNumbers(poLine), is(List.of()));
  }

  @Test
  void getVendorOrderNumberReturnsNullWhenReferenceNumberItemsIsNull() {
    assertThat(ExportUtils.getVendorOrderNumber((List<ReferenceNumberItem>) null), is(nullValue()));
  }

  @Test
  void getVendorOrderNumberReturnsNullWhenNoOrderReferenceNumber() {
    List<ReferenceNumberItem> referenceNumberItems = List.of(new ReferenceNumberItem());
    assertThat(ExportUtils.getVendorOrderNumber(referenceNumberItems), is(nullValue()));
  }

  @Test
  void getVendorOrderNumberReturnsOrderReferenceNumber() {
    ReferenceNumberItem item = new ReferenceNumberItem();
    item.setRefNumberType(ReferenceNumberItem.RefNumberTypeEnum.ORDER_REFERENCE_NUMBER);
    item.setRefNumber("12345");
    List<ReferenceNumberItem> referenceNumberItems = List.of(item);
    assertThat(ExportUtils.getVendorOrderNumber(referenceNumberItems).getRefNumber(), is("12345"));
  }

  @Test
  void getVendorAccountNumberReturnsNullWhenVendorDetailIsNull() {
    PoLine poLine = new PoLine();
    assertThat(ExportUtils.getVendorAccountNumber(poLine), is(nullValue()));
  }

  @Test
  void getFormattedDateReturnsEmptyStringWhenDateIsPopulated() {
    assertThat(ExportUtils.getFormattedDate(new Date()), notNullValue());
  }

  @Test
  void getFormattedDateReturnsEmptyStringWhenDateIsNull() {
    assertThat(ExportUtils.getFormattedDate(null), is(""));
  }

  @ParameterizedTest
  @CsvSource({"ORDERING,EDI", "CLAIMING,EDI", "CLAIMING,CSV"})
  void generateFileNameGeneratesCorrectFileName(VendorEdiOrdersExportConfig.IntegrationTypeEnum integrationType, VendorEdiOrdersExportConfig.FileFormatEnum fileFormat) {
    var fileName = ExportUtils.generateFileName("vendor", "config", integrationType, fileFormat);
    assertThat(fileName.matches(getRegexPattern(integrationType, fileFormat)), is(true));
  }

  private String getRegexPattern(VendorEdiOrdersExportConfig.IntegrationTypeEnum integrationType, VendorEdiOrdersExportConfig.FileFormatEnum fileFormat) {
    if (integrationType == VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING) {
      if (fileFormat == VendorEdiOrdersExportConfig.FileFormatEnum.EDI) {
        return "^edi.vendor_config_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.edi$";
      } else {
        return "^vendor_config_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.csv$";
      }
    }
    return  "^vendor_config_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.edi$";
  }

  @Test
  void convertIntegrationTypeToExportTypeReturnsCorrectExportType() {
    assertThat(ExportUtils.convertIntegrationTypeToExportType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING), is(ExportType.EDIFACT_ORDERS_EXPORT));
    assertThat(ExportUtils.convertIntegrationTypeToExportType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING), is(ExportType.CLAIMS));
  }
}
