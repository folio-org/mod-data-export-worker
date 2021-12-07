package org.folio.dew.client;

import org.folio.dew.domain.dto.AddressType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "addresstypes")
public interface AddressTypeClient {

  @GetMapping(value = "/{typeId}", produces = MediaType.APPLICATION_JSON_VALUE)
  AddressType getAddressTypeById(@PathVariable String typeId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  AddressType getAddressTypeByQuery(@RequestParam String query);

}
