package org.folio.dew.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocaleClientTest {

  private static final String LOCALE_JSON = """
    {
      "locale": "en-US",
      "currency": "USD",
      "timezone": "America/New_York",
      "numberingSystem": "latn"
    }
    """;

  @InjectMocks
  private LocaleClient localeClient;

  @Mock
  private LocaleClient.LocaleClientRaw underlyingClient;

  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldReturnLocaleSettingsWhenResponseIsValid() {
    when(underlyingClient.getLocaleSettings()).thenReturn(LOCALE_JSON);

    var expected = new LocaleClient.LocaleSettings("en-US", "USD", "America/New_York", "latn");
    assertThat(localeClient.getLocaleSettings()).isEqualTo(expected);
    assertThat(localeClient.getLocaleSettings().getZoneId().getId()).isEqualTo("America/New_York");
  }

  @Test
  void shouldReturnDefaultSettingsWhenClientThrowsException() {
    when(underlyingClient.getLocaleSettings())
      .thenThrow(new FeignException.Unauthorized("", mock(feign.Request.class), null, null));

    var expectedDefault = new LocaleClient.LocaleSettings("en-US", "USD", "UTC", "latn");
    assertThat(localeClient.getLocaleSettings()).isEqualTo(expectedDefault);
  }

  @Test
  void shouldReturnDefaultSettingsWhenJsonIsInvalid() {
    when(underlyingClient.getLocaleSettings()).thenReturn("invalid json");

    var expectedDefault = new LocaleClient.LocaleSettings("en-US", "USD", "UTC", "latn");
    assertThat(localeClient.getLocaleSettings()).isEqualTo(expectedDefault);
  }

  @Test
  void shouldFallbackToUtcWhenTimezoneIsInvalid() {
    var settings = new LocaleClient.LocaleSettings("en-US", "USD", "not-a-timezone", "latn");

    assertThat(settings.getZoneId().getId()).isEqualTo("UTC");
  }
}
