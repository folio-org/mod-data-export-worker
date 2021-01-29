package org.folio.dto;

import lombok.Data;
import org.springframework.batch.core.BatchStatus;

import java.util.Date;

@Data
public class JobExecutionDto {

    private BatchStatus status;

    private Date startTime;

    private Date createTime;

    private Date endTime;

    private Date lastUpdated;
}
