package org.folio.de.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import org.folio.de.entity.converters.EntityTypeConverter;
import org.folio.de.entity.converters.ExportTypeConverter;
import org.folio.de.entity.converters.IdentifierTypeConverter;
import org.folio.de.entity.converters.JobCommandTypeConverter;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.IdentifierType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.batch.core.JobParameters;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Data
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class JobCommand {
  @Id
  private UUID id;
  @Convert(converter = JobCommandTypeConverter.class)
  private JobCommandType jobCommandType;
  private String name;
  private String description;
  @Convert(converter = ExportTypeConverter.class)
  private ExportType exportType;
  @Type(type = "jsonb")
  private JobParameters jobParameters;
  @Convert(converter = IdentifierTypeConverter.class)
  private IdentifierType identifierType;
  @Convert(converter = EntityTypeConverter.class)
  private EntityType entityType;

}
