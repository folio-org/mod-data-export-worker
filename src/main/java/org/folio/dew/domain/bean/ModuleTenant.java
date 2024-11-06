package org.folio.dew.domain.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModuleTenant {

  @JsonProperty("id")
  private String id;
  @JsonProperty("name")
  private String name;
}
