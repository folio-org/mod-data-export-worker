package org.folio.dew.client;

import org.folio.dew.domain.dto.circulationlog.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;


@HttpExchange(url = "locale")
public interface LocaleClient {

  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  Locale getLocale();
}
