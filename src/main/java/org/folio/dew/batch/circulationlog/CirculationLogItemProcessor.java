package org.folio.dew.batch.circulationlog;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.LocaleClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.domain.dto.CirculationLogExportFormat;
import org.folio.dew.domain.dto.LogRecord;
import org.folio.dew.domain.dto.LogRecordItemsInner;
import org.folio.dew.domain.dto.ServicePoint;
import org.folio.dew.domain.dto.circulationlog.Locale;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class CirculationLogItemProcessor implements ItemProcessor<LogRecord, CirculationLogExportFormat> {

  private final ServicePointClient servicePointClient;
  private final LocaleClient localeClient;

  private Map<String, String> servicePointMap;
  private SimpleDateFormat format;

  @Override
  public CirculationLogExportFormat process(LogRecord item) {
    final String servicePointName = servicePointMap.get(item.getServicePointId());
    var logExportFormat = new CirculationLogExportFormat();
    logExportFormat.setUserBarcode(item.getUserBarcode());
    logExportFormat.setDescription(item.getDescription());
    logExportFormat.setAction(item.getAction().getValue());
    logExportFormat.setDate(format.format(item.getDate()));
    logExportFormat.setServicePointId(servicePointName);
    logExportFormat.setObjectField(item.getObject().getValue());
    logExportFormat.setSource(item.getSource());

    final String items = item.getItems().stream()
      .filter(lri -> lri != null && StringUtils.isNotBlank(lri.getItemBarcode()))
      .map(LogRecordItemsInner::getItemBarcode)
      .collect(Collectors.joining(","));
    logExportFormat.setItems(items);
    return logExportFormat;
  }


  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    fetchServicePoints();
    initTenantSpecificDateFormat();
  }

  private void initTenantSpecificDateFormat() {
    if (format != null) return;

    String timezoneId = fetchTimezone();

    var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    dateFormat.setTimeZone(TimeZone.getTimeZone(timezoneId));

    format = dateFormat;
  }

  private String fetchTimezone() {
    try {
      Locale localeSettings = localeClient.getLocale();

      if (localeSettings != null) {
        return ofNullable(localeSettings.getTimezone()).orElse("UTC");
      }
      log.warn("Timezone not found in locale settings, using default UTC");
    } catch (Exception e) {
      log.warn("Failed to fetch timezone from locale endpoint: {}", e.getMessage());
    }

    return "UTC";
  }

  private void fetchServicePoints() {
    if (servicePointMap != null) return;

    var servicePoints = servicePointClient.get("name<>null", 1000);

    servicePointMap = servicePoints.getServicepoints().isEmpty() ?
      Collections.emptyMap() :
      servicePoints.getServicepoints().stream()
        .collect(Collectors.toMap(ServicePoint::getId, ServicePoint::getName));
  }

}
