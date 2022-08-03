package org.folio.de.entity.converters;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.IdentifierType;

import javax.persistence.AttributeConverter;
import java.util.Objects;

public class IdentifierTypeConverter implements AttributeConverter<IdentifierType, String> {

  @Override
  public String convertToDatabaseColumn(IdentifierType identifierType) {
    if (Objects.isNull(identifierType)) {
      return StringUtils.EMPTY;
    }
    return identifierType.getValue();
  }

  @Override
  public IdentifierType convertToEntityAttribute(String value) {
    if (StringUtils.isEmpty(value)) {
      return null;
    }
    return IdentifierType.fromValue(value);
  }
}
