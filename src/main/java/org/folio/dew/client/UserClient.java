package org.folio.dew.client;

import org.folio.dew.domain.dto.UserCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "users")
public interface UserClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  UserCollection getUserByQuery(@RequestParam("query") String query, @RequestParam long limit);
}
