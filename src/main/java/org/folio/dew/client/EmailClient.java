package org.folio.dew.client;

import org.folio.dew.domain.dto.email.EmailEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "email", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface EmailClient {

  @PostExchange
  void sendEmail(@RequestBody EmailEntity email);

}
