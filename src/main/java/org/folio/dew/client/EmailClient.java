package org.folio.dew.client;


import org.folio.dew.domain.dto.email.EmailEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "email")
public interface EmailClient {

  @PostMapping(value = "/email", consumes = MediaType.APPLICATION_JSON_VALUE)
  void sendEmail(@RequestBody EmailEntity email);

}
