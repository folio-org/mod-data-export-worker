package org.folio.dew.client;

import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "users", accept = MediaType.APPLICATION_JSON_VALUE)
public interface UserClient {

  @GetExchange
  UserCollection getUserByQuery(@RequestParam("query") String query, @RequestParam long limit);

  @GetExchange(value = "/{userId}")
  User getUserById(@PathVariable String userId);

}
