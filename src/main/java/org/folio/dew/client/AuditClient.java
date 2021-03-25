package org.folio.dew.client;

import org.folio.dew.domain.dto.LogRecordCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("audit-data")
public interface AuditClient {

  @GetMapping(value = "/circulation/logs", produces = MediaType.APPLICATION_JSON_VALUE)
  LogRecordCollection getCirculationAuditLogs(@RequestParam String query, @RequestParam int offset, @RequestParam int limit,
      @RequestParam String lang);

}
