package org.folio.dew.client;


import org.folio.dew.domain.dto.email.EmailEntity;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "email", url = "http://email", configuration = FeignClientConfiguration.class)
public interface EmailClient {

  @PostMapping(value = "/email", consumes = MediaType.APPLICATION_JSON_VALUE)
  void sendEmail(@RequestBody EmailEntity email);

}
