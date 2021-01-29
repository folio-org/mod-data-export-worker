package org.folio.dto;

import lombok.Data;
import org.folio.model.entities.enums.JobType;

@Data
public class BaseJobDto {

    private String name;

    private String description;

    private JobType jobType;
}
