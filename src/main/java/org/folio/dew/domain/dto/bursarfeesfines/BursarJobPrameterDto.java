package org.folio.dew.domain.dto.bursarfeesfines;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import lombok.Data;


@Data
public class BursarJobPrameterDto implements Serializable {
  private Integer daysOutstanding;
  private List<String> patronGroups;
  private UUID servicePointId;
  private UUID feefineOwnerId;
  private UUID transferAccountId;
  private String typeMappings;
}
