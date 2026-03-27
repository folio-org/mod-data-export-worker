package org.folio.dew.client;

import org.folio.dew.domain.dto.ExportConfigCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "data-export-spring")
public interface DataExportSpringClient {

  @GetExchange(value = "/configs", accept = MediaType.APPLICATION_JSON_VALUE)
  ExportConfigCollection getExportConfigs(@RequestParam("query") String query);

}
