package org.folio.dew.client;

import org.folio.dew.batch.bulkedit.jobs.permissions.check.UserPermissions;
import org.folio.dew.config.feign.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "perms/users", configuration = FeignClientConfiguration.class)
public interface UserPermissionsClient {

  @GetMapping(value = "/{userId}/permissions?expanded=true&indexField=userId", produces = MediaType.APPLICATION_JSON_VALUE)
  UserPermissions getPermissions(@PathVariable String userId);
}
