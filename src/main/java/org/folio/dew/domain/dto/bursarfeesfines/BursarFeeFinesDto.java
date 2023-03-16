package org.folio.dew.domain.dto.bursarfeesfines;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.folio.dew.domain.dto.BursarFeeFinesTypeMapping;

@Data
@NoArgsConstructor
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
