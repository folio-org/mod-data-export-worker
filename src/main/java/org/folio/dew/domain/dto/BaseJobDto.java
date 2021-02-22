package org.folio.dew.domain.dto;

import lombok.Data;
import org.folio.dew.domain.entity.enums.JobType;

@Data
public class BaseJobDto {

  private String name;
  private String description;
  private JobType jobType;

}
