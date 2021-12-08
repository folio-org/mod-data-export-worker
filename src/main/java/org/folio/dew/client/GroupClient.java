package org.folio.dew.client;

import org.folio.dew.domain.dto.UserGroup;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "groups")
public interface GroupClient {

  @GetMapping(value = "/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
  UserGroup getGroupById(@PathVariable String groupId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  UserGroup getGroupByQuery(@RequestParam String query);
}
