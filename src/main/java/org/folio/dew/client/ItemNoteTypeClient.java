package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.NoteType;
import org.folio.dew.domain.dto.NoteTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "item-note-types", configuration = FeignClientConfiguration.class)
public interface ItemNoteTypeClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  NoteType getById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  NoteTypeCollection getByQuery(@RequestParam String query);
}
