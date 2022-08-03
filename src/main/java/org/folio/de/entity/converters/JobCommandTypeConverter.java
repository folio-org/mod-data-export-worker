package org.folio.de.entity.converters;

import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.JobCommandType;

import javax.persistence.AttributeConverter;
import java.util.Objects;

public class JobCommandTypeConverter implements AttributeConverter<JobCommandType, String> {

  @Override
  public String convertToDatabaseColumn(JobCommandType jobCommandType) {
    if (Objects.isNull(jobCommandType)) {
      return StringUtils.EMPTY;
    }
    return jobCommandType.getValue();
  }

  @Override
  public JobCommandType convertToEntityAttribute(String value) {
    if (StringUtils.isEmpty(value)) {
      return null;
    }
    return JobCommandType.fromValue(value);
  }
}
