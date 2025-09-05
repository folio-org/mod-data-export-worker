package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;


@FeignClient(name = "settings/entries", configuration = FeignClientConfiguration.class)
public interface SettingsClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  Map<String, Object> getSettings(@RequestParam("query") String query);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  Map<String, Object> getSettings(@RequestParam("query") String query,
                                  @RequestParam("userId") String userId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/{entryId}")
  Map<String, Object> getSettingById(@PathVariable String entryId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/{entryId}")
  Map<String, Object> getSettingById(@PathVariable String entryId,
                                     @RequestParam("userId") String userId);
}
