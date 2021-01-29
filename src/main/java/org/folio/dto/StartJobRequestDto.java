package org.folio.dto;

import lombok.Data;

import java.util.Map;

@Data
public class StartJobRequestDto extends BaseJobDto {

    private Map<String, JobParameterDto> jobInputParameters;
}
