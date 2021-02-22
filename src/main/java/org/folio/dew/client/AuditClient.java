package org.folio.dew.client;

import org.folio.dew.domain.dto.LogRecordCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("audit-data")
public interface AuditClient {

  @GetMapping(value = "/circulation/logs", produces = MediaType.APPLICATION_JSON_VALUE)
  LogRecordCollection getCirculationAuditLogs(String query, int offset, int limit, String lang);

}
