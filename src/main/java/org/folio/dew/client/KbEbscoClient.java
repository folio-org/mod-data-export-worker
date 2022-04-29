package org.folio.dew.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.dew.domain.dto.EHoldingsPackage;
import org.folio.dew.domain.dto.EHoldingsTitle;

@FeignClient(name = "eholdings")
public interface KbEbscoClient {

  @GetMapping(value = "/packages/{packageId}", produces = MediaType.APPLICATION_JSON_VALUE)
  EHoldingsPackage getPackageById(@PathVariable String packageId);

  @GetMapping(value = "/titles/{titleId}", produces = MediaType.APPLICATION_JSON_VALUE)
  EHoldingsTitle getTitleById(@PathVariable String titleId);

  @GetMapping(value = "/packages/{packageId}/resources?{query}",produces = MediaType.APPLICATION_JSON_VALUE)
  List<EHoldingsTitle> getResourcesByPackageId(@PathVariable String packageId, @PathVariable String query);
}


