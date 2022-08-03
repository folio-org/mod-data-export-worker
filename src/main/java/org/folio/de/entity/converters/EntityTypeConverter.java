package org.folio.de.entity.converters;


import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.EntityType;

import javax.persistence.AttributeConverter;
import java.util.Objects;

public class EntityTypeConverter implements AttributeConverter<EntityType, String> {

  @Override
  public String convertToDatabaseColumn(EntityType entityType) {
    if (Objects.isNull(entityType)) {
      return StringUtils.EMPTY;
    }
    return entityType.getValue();
  }

  @Override
  public EntityType convertToEntityAttribute(String value) {
    if (StringUtils.isEmpty(value)) {
      return null;
    }
    return EntityType.fromValue(value);
  }
}
