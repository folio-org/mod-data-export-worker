package org.folio.dew.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.config.feign.FeignClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Client for the /locale API.
 *
 * @implNote This is a separate class from the internal LocaleClientRaw interface because Feign clients must be interfaces, disallowing any injection.
 */
@Log4j2
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LocaleClient {

  private final ObjectMapper objectMapper;
  private final LocaleClientRaw underlyingClient;

  /**
   * Provides raw access to the /locale API.
   */
  @FeignClient(name = "locale", configuration = FeignClientConfiguration.class)
  interface LocaleClientRaw {
    @GetMapping
    String getLocaleSettings();
  }

  public record LocaleSettings(
    String locale,
    String currency,
    String timezone,
    String numberingSystem
  ) {
    public ZoneId getZoneId() {
      try {
        return ZoneId.of(timezone);
      } catch (DateTimeException e) {
        log.error("Invalid timezone '{}', defaulting to UTC.", timezone, e);
        return ZoneId.of("UTC");
      }
    }
  }

  public LocaleSettings getLocaleSettings() {
    try {
      String response = underlyingClient.getLocaleSettings();
      return objectMapper.readValue(response, LocaleSettings.class);
    } catch (JsonProcessingException | FeignException | NullPointerException e) {
      log.error("Failed to retrieve locale information. Defaulting to en-US, USD, UTC, latn.", e);
      return new LocaleSettings("en-US", "USD", "UTC", "latn");
    }
  }
}
