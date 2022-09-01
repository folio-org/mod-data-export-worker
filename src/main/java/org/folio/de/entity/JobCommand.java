package org.folio.de.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.IdentifierType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.batch.core.JobParameters;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import java.util.UUID;

@Data
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class JobCommand {
  @Id
  private UUID id;
  @Enumerated(EnumType.STRING)
  @Column(name = "job_command_type")
  private JobCommandType type;
  private String name;
  private String description;
  @Enumerated(EnumType.STRING)
  private ExportType exportType;
  @Type(type = "jsonb")
  private JobParameters jobParameters;
  @Enumerated(EnumType.STRING)
  private IdentifierType identifierType;
  @Enumerated(EnumType.STRING)
  private EntityType entityType;

}
