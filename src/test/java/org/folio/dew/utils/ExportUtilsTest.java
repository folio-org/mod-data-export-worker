package org.folio.dew.utils;

import org.folio.dew.CopilotGenerated;
import org.folio.dew.batch.acquisitions.utils.ExportUtils;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.ReferenceNumberItem;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.Test;

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
    CompositePoLine poLine = new CompositePoLine();
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
    CompositePoLine poLine = new CompositePoLine();
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

  @Test
  void generateFileNameGeneratesCorrectFileName() {
    String vendorName = "vendor";
    String configName = "config";
    VendorEdiOrdersExportConfig.FileFormatEnum fileFormat = VendorEdiOrdersExportConfig.FileFormatEnum.EDI;
    String fileName = ExportUtils.generateFileName(vendorName, configName, fileFormat);
    assertThat(fileName.matches("^edi.vendor_config_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.edi$"), is(true));
  }

  @Test
  void convertIntegrationTypeToExportTypeReturnsCorrectExportType() {
    assertThat(ExportUtils.convertIntegrationTypeToExportType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING), is(ExportType.EDIFACT_ORDERS_EXPORT));
    assertThat(ExportUtils.convertIntegrationTypeToExportType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING), is(ExportType.CLAIMS));
  }
}
