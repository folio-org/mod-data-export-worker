package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "users", configuration = FeignClientConfiguration.class)
public interface UserClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  UserCollection getUserByQuery(@RequestParam("query") String query, @RequestParam long limit);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  UserCollection getUserByQuery(@RequestParam("query") String query, @RequestParam long offset, @RequestParam long limit);

  @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
  User getUserById(@PathVariable String userId);

}
