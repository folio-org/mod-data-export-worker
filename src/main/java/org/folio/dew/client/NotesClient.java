package org.folio.dew.client;

import java.util.List;
import lombok.Data;
import lombok.Getter;
import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.eholdings.Note;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "note-links", configuration = FeignClientConfiguration.class)
public interface NotesClient {

  @GetMapping(value = "/domain/{domain}/type/{type}/id/{id}?status=assigned&orderBy=updatedDate",
              produces = MediaType.APPLICATION_JSON_VALUE)
  NoteCollection getAssignedNotes(@PathVariable("domain") NoteLinkDomain domain,
                                  @PathVariable("type") NoteLinkType type,
                                  @PathVariable("id") String entityId);

  @Getter
  enum NoteLinkDomain {

    EHOLDINGS("eholdings");

    private final String val;

    NoteLinkDomain(String val) {
      this.val = val;
    }
  }

  @Getter
  enum NoteLinkType {

    PACKAGE("package"),
    RESOURCE("resource");

    private final String val;

    NoteLinkType(String val) {
      this.val = val;
    }
  }

  @Data
  class NoteCollection {

    List<Note> notes;

    int totalRecords;
  }

  @Component
  class NoteLinkTypeToPathVarConverter implements Converter<NoteLinkType, String> {

    @Override
    public String convert(NoteLinkType source) {
      return source.getVal();
    }
  }

  @Component
  class NoteLinkDomainToPathVarConverter implements Converter<NoteLinkDomain, String> {

    @Override
    public String convert(NoteLinkDomain source) {
      return source.getVal();
    }
  }
}
