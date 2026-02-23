package org.folio.dew.batch.circulationlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.folio.dew.client.LocaleClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.domain.dto.ActionType;
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

@ExtendWith(MockitoExtension.class)
class CirculationLogItemProcessorTest {

  @Mock
  private ServicePointClient servicePointClient;
  @Mock
  private LocaleClient localeClient;

  @InjectMocks
  private CirculationLogItemProcessor processor;

  @Test
  void shouldUseLocaleTimezoneAndReuseCachedValuesAcrossInit() throws Exception {
    var servicepoints = new Servicepoints();
    servicepoints.setServicepoints(List.of(new ServicePoint().id("sp-1").name("Main service point")));
    when(servicePointClient.get("name<>null", 1000)).thenReturn(servicepoints);
    when(localeClient.getLocaleSettings()).thenReturn(new LocaleClient.LocaleSettings("en-US", "USD", "America/New_York", "latn"));

    processor.initStep(null);
    processor.initStep(null);

    verify(servicePointClient, times(1)).get("name<>null", 1000);
    verify(localeClient, times(1)).getLocaleSettings();

    var item = new LogRecord()
      .userBarcode("u-1")
      .description("desc")
      .action(ActionType.CHECK_OUT)
      .date(java.util.Date.from(Instant.parse("2024-01-01T00:00:00Z")))
      .servicePointId("sp-1")
      ._object(LoggedObjectType.LOAN)
      .source("SYSTEM")
      .items(Arrays.asList(
        new LogRecordItemsInner().itemBarcode("bc-1"),
        new LogRecordItemsInner().itemBarcode(" "),
        null,
        new LogRecordItemsInner().itemBarcode("bc-2")));

    var result = processor.process(item);

    assertThat(result.getServicePointId()).isEqualTo("Main service point");
    assertThat(result.getDate()).isEqualTo("2023-12-31 19:00");
    assertThat(result.getItems()).isEqualTo("bc-1,bc-2");
    assertThat(result.getAction()).isEqualTo("Check out");
    assertThat(result.getObjectField()).isEqualTo("Loan");
  }

  @Test
  void shouldHandleEmptyServicePoints() throws Exception {
    var servicepoints = new Servicepoints();
    servicepoints.setServicepoints(List.of());
    when(servicePointClient.get("name<>null", 1000)).thenReturn(servicepoints);
    when(localeClient.getLocaleSettings()).thenReturn(new LocaleClient.LocaleSettings("en-US", "USD", "UTC", "latn"));

    processor.initStep(null);

    var item = new LogRecord()
      .userBarcode("u-1")
      .description("desc")
      .action(ActionType.CHECK_IN)
      .date(java.util.Date.from(Instant.parse("2024-01-01T00:00:00Z")))
      .servicePointId("missing")
      ._object(LoggedObjectType.LOAN)
      .source("SYSTEM")
      .items(List.of(new LogRecordItemsInner().itemBarcode("bc-1")));

    var result = processor.process(item);

    assertThat(result.getServicePointId()).isNull();
    assertThat(result.getDate()).isEqualTo("2024-01-01 00:00");
  }
}
