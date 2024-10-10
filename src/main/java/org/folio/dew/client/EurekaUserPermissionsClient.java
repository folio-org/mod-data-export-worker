package org.folio.dew.client;

import org.folio.dew.batch.bulkedit.jobs.permissions.check.UserPermissions;
import org.folio.dew.config.feign.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "users-keycloak/users", configuration = FeignClientConfiguration.class)
public interface EurekaUserPermissionsClient {

  @GetMapping(value = "/{userId}/permissions", produces = MediaType.APPLICATION_JSON_VALUE)
  UserPermissions getPermissions(@PathVariable String userId, @RequestParam List<String> desiredPermissions);
}
