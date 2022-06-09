package org.folio.dew.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.folio.dew.config.feign.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "erm", configuration = FeignClientConfiguration.class)
public interface AgreementClient {
  String ITEMS_REFERENCE_FILTER = "items.reference";

  static String getFiltersParam(String id) {
    return ITEMS_REFERENCE_FILTER + "=" + id;
  }

  @GetMapping(value = "/sas", produces = MediaType.APPLICATION_JSON_VALUE)
  List<Agreement> getAssignedAgreements(@RequestParam(value = "filters") String filters);

  @Data
  class Agreement {

    private String status;
    private String name;
    private String startDate;

    @JsonProperty("agreementStatus")
    private void unpackStatusFromNestedObject(Map<String, String> agreementStatus) {
      status = agreementStatus.get("label");
    }
  }
}
