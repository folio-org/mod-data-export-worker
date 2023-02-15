package org.folio.dew.domain.dto.bursarfeesfines;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NonNull;
import org.folio.dew.domain.dto.BursarFeeFinesTypeMapping;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class BursarFeeFinesDto {

  @JsonProperty("daysOutstanding")
  @Min(0)
  @NonNull
  private Integer daysOutstanding;

  @JsonProperty("patronGroups")
  @Size(min=1)
  @NotNull
  private List<String> patronGroups = new ArrayList<>();

  @JsonProperty("servicePointId")
  private UUID servicePointId;

  @JsonProperty("feefineOwnerId")
  private UUID feefineOwnerId;

  @JsonProperty("transferAccountId")
  private UUID transferAccountId;

  @JsonProperty("typeMappings")
  @Valid
  private Map<String, List<BursarFeeFinesTypeMapping>> typeMappings = null;
}
