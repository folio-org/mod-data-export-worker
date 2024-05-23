package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.HoldingsNoteType;
import org.folio.dew.domain.dto.HoldingsNoteTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "holdings-note-types", configuration = FeignClientConfiguration.class)
public interface HoldingsNoteTypeClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsNoteType getById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsNoteTypeCollection getByQuery(@RequestParam String query);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsNoteTypeCollection getNoteTypes(@RequestParam("limit") int limit);
}
