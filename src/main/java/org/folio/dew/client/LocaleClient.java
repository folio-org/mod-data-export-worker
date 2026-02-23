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
      } catch (DateTimeException | NullPointerException e) {
        log.error("Invalid or missing timezone '{}', defaulting to UTC.", timezone, e);
        return ZoneId.of("UTC");
      }
    }
  }

  public LocaleSettings getLocaleSettings() {
    try {
      String response = underlyingClient.getLocaleSettings();
      LocaleSettings settings = objectMapper.readValue(response, LocaleSettings.class);
      log.info("Retrieved tenant locale settings from /locale: locale='{}', timezone='{}', currency='{}', numberingSystem='{}'.",
        settings.locale(), settings.timezone(), settings.currency(), settings.numberingSystem());
      return settings;
    } catch (FeignException e) {
      log.warn("Failed to call /locale (status={}): {}. Falling back to defaults.", e.status(), e.getMessage());
      return defaultLocaleSettings();
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse /locale response. Falling back to defaults.", e);
      return defaultLocaleSettings();
    } catch (NullPointerException e) {
      log.warn("Missing locale settings payload from /locale. Falling back to defaults.", e);
      return defaultLocaleSettings();
    }
  }

  private LocaleSettings defaultLocaleSettings() {
    return new LocaleSettings("en-US", "USD", "UTC", "latn");
  }
}
