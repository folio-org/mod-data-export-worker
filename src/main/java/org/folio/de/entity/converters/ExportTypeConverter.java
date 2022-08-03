package org.folio.de.entity.converters;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.ExportType;

import javax.persistence.AttributeConverter;
import java.util.Objects;

public class ExportTypeConverter implements AttributeConverter<ExportType, String> {

  @Override
  public String convertToDatabaseColumn(ExportType exportType) {
    if (Objects.isNull(exportType)) {
      return StringUtils.EMPTY;
    }
    return exportType.getValue();
  }

  @Override
  public ExportType convertToEntityAttribute(String value) {
    if (StringUtils.isEmpty(value)) {
      return null;
    }
    return ExportType.fromValue(value);
  }
}
