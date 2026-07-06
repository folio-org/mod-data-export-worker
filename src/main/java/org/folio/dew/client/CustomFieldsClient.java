package org.folio.dew.client;

import org.folio.dew.domain.dto.acquisitions.customfields.CustomFieldCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Client for the {@code custom-fields} interface. Because {@code custom-fields} is declared as an
 * {@code interfaceType: multiple} interface (provided by several modules), Kong cannot route the
 * request on path alone — the {@code X-Okapi-Module-Id} header must name the target module
 * (e.g. {@code mod-orders-storage}).
 */
@HttpExchange(url = "custom-fields", accept = MediaType.APPLICATION_JSON_VALUE)
public interface CustomFieldsClient {

  @GetExchange
  CustomFieldCollection getCustomFields(@RequestParam("query") String query,
                                        @RequestParam("limit") int limit,
                                        @RequestHeader("X-Okapi-Module-Id") String moduleId);
}