package org.folio.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class JobExecutionUpdateDto extends JobExecutionDto {

    private UUID jobId;
}
