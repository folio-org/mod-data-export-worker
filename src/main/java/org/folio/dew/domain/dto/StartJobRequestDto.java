package org.folio.dew.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class StartJobRequestDto extends BaseJobDto {

  private Map<String, JobParameterDto> jobInputParameters;

}
