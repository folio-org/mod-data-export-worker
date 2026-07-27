package org.folio.dew.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Entitlements {

  private List<Entitlement> results;
  private Integer total;
  private Integer totalPages;
  private Integer pageSize;
  private Integer page;

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Entitlement {
    private String reference;
    private Owner owner;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Owner {
    private String id;
    private String name;
    private String startDate;
    private Map<String, String> agreementStatus;
  }
}
