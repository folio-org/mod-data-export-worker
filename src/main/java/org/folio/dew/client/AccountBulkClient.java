package org.folio.dew.client;

import org.folio.dew.domain.dto.bursarfeesfines.TransferRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "accounts-bulk")
public interface AccountBulkClient {

  @PostExchange(value = "/transfer", accept = MediaType.APPLICATION_JSON_VALUE)
  void transferAccount(@RequestBody TransferRequest request);
}
