package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.UserGroup;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "groups", configuration = FeignClientConfiguration.class)
public interface GroupClient {

  @GetMapping(value = "/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
  UserGroup getGroupById(@PathVariable String groupId);

}
