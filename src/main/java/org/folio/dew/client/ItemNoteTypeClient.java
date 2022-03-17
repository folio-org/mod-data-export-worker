package org.folio.dew.client;

import org.folio.dew.domain.dto.NoteType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "item-note-types")
public interface ItemNoteTypeClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  NoteType getById(@PathVariable String id);
}
