package org.folio.dew.client;

import org.folio.dew.domain.dto.LogRecordCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("audit-data")
public interface AuditClient {

  @GetExchange(value = "/circulation/logs", accept = MediaType.APPLICATION_JSON_VALUE)
  LogRecordCollection getCirculationAuditLogs(@RequestParam(required = false) String query, @RequestParam int offset, @RequestParam int limit,
      @RequestParam(required = false) String lang);

}
