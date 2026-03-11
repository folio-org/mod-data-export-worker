package org.folio.dew.client;

import java.util.List;
import java.util.Optional;

import lombok.Data;
import lombok.Getter;
import org.folio.dew.domain.dto.eholdings.Note;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "note-links")
public interface NotesClient {

  @GetExchange(value = "/domain/{domain}/type/{type}/id/{id}?status=assigned&orderBy=updatedDate",
          accept = MediaType.APPLICATION_JSON_VALUE)
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
