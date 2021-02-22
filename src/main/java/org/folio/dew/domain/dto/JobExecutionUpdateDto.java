package org.folio.dew.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class JobExecutionUpdateDto extends JobExecutionDto {

  private UUID jobId;

}
