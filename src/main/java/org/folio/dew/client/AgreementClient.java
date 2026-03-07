package org.folio.dew.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "erm")
public interface AgreementClient {
  String ITEMS_REFERENCE_FILTER = "items.reference";

  static String getFiltersParam(String id) {
    return ITEMS_REFERENCE_FILTER + "=" + id;
  }

  @GetExchange(value = "/sas", accept = MediaType.APPLICATION_JSON_VALUE)
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
