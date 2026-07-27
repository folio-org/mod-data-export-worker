package org.folio.dew.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.folio.dew.client.dto.Entitlements;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "erm")
public interface AgreementClient {
  String ITEMS_REFERENCE_FILTER = "items.reference";
  String EKB_TITLE_AUTHORITY = "EKB-TITLE";

  static String getFiltersParam(String id) {
    return ITEMS_REFERENCE_FILTER + "=" + id;
  }

  static String getEntitlementsFilterParam(String packageId) {
    return "reference=~" + packageId + "-&&authority=" + EKB_TITLE_AUTHORITY;
  }

  @GetExchange(value = "/sas", accept = MediaType.APPLICATION_JSON_VALUE)
  List<Agreement> getAssignedAgreements(@RequestParam(value = "filters") String filters);

  @GetExchange(value = "/entitlements", accept = MediaType.APPLICATION_JSON_VALUE)
  Entitlements getEntitlements(@RequestParam(value = "filters") String filters,
                               @RequestParam(value = "fetchExternalResources") boolean fetchExternalResources,
                               @RequestParam(value = "stats") boolean stats,
                               @RequestParam(value = "perPage") int perPage,
                               @RequestParam(value = "page") int page);

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
