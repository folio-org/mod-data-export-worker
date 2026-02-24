package org.folio.dew.batch.circulationlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.folio.dew.client.LocaleClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.domain.dto.ActionType;
import org.folio.dew.domain.dto.CirculationLogExportFormat;
import org.folio.dew.domain.dto.LogRecord;
import org.folio.dew.domain.dto.LogRecordItemsInner;
import org.folio.dew.domain.dto.LoggedObjectType;
import org.folio.dew.domain.dto.ServicePoint;
import org.folio.dew.domain.dto.Servicepoints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.test.MetaDataInstanceFactory;

@ExtendWith(MockitoExtension.class)
class CirculationLogItemProcessorTest {

  private static final String SP_ID = "sp-001";
  private static final String SP_NAME = "Main Circ Desk";
  private static final String USER_BARCODE = "1234567890";
  private static final Date EVENT_DATE = new Date(1700000000000L);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Mock
  private ServicePointClient servicePointClient;
  @Mock
  private LocaleClient localeClient;

  @InjectMocks
  private CirculationLogItemProcessor processor;

  @Test
  void process_allFields_returnsMappedFormat() {
    initProcessor("UTC");

    LogRecord record = createLogRecord(List.of(
      new LogRecordItemsInner().itemBarcode("ITEM-1"),
      new LogRecordItemsInner().itemBarcode("ITEM-2")));

    CirculationLogExportFormat result = processor.process(record);

    assertThat(result.getUserBarcode()).isEqualTo(USER_BARCODE);
    assertThat(result.getAction()).isEqualTo(ActionType.CHECK_OUT.getValue());
    assertThat(result.getObjectField()).isEqualTo(LoggedObjectType.LOAN.getValue());
    assertThat(result.getServicePointId()).isEqualTo(SP_NAME);
    assertThat(result.getItems()).isEqualTo("ITEM-1,ITEM-2");
    assertThat(result.getDate()).isEqualTo(formatDate(EVENT_DATE, "UTC"));
  }

  @Test
  void process_nullAndBlankBarcodes_filteredOut() {
    initProcessor("UTC");

    List<LogRecordItemsInner> items = new java.util.ArrayList<>();
    items.add(new LogRecordItemsInner().itemBarcode("VALID"));
    items.add(new LogRecordItemsInner().itemBarcode("  "));
    items.add(new LogRecordItemsInner().itemBarcode(null));
    items.add(null);

    CirculationLogExportFormat result = processor.process(createLogRecord(items));

    assertThat(result.getItems()).isEqualTo("VALID");
  }

  @Test
  void initStep_localeFetchFails_defaultsToUtc() {
    when(servicePointClient.get("name<>null", 1000)).thenReturn(createServicepoints());
    when(localeClient.getLocale()).thenThrow(new RuntimeException("Connection refused"));

    processor.initStep(MetaDataInstanceFactory.createStepExecution());

    CirculationLogExportFormat result = processor.process(createLogRecord(List.of()));

    assertThat(result.getDate()).isEqualTo(formatDate(EVENT_DATE, "UTC"));
  }

  // -- Helpers --

  private void initProcessor(String timezoneId) {
    ObjectNode locale = MAPPER.createObjectNode().put("timezone", timezoneId);
    when(servicePointClient.get("name<>null", 1000)).thenReturn(createServicepoints());
    when(localeClient.getLocale()).thenReturn(locale);
    processor.initStep(MetaDataInstanceFactory.createStepExecution());
  }

  private static Servicepoints createServicepoints() {
    ServicePoint sp = new ServicePoint();
    sp.setId(SP_ID);
    sp.setName(SP_NAME);
    return new Servicepoints().servicepoints(List.of(sp)).totalRecords(1);
  }

  private static LogRecord createLogRecord(List<LogRecordItemsInner> items) {
    return new LogRecord()
      .userBarcode(USER_BARCODE)
      .date(EVENT_DATE)
      .servicePointId(SP_ID)
      .action(ActionType.CHECK_OUT)
      ._object(LoggedObjectType.LOAN)
      .source("System")
      .description("Test")
      .items(items);
  }

  private static String formatDate(Date date, String tz) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    sdf.setTimeZone(TimeZone.getTimeZone(tz));
    return sdf.format(date);
  }
}
