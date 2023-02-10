package org.folio.de.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.IdentifierType;
import org.hibernate.annotations.Type;
import org.springframework.batch.core.JobParameters;

import java.util.UUID;

@Data
@Entity
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
  @Type(JsonBinaryType.class)
  private JobParameters jobParameters;
  @Enumerated(EnumType.STRING)
  private IdentifierType identifierType;
  @Enumerated(EnumType.STRING)
  private EntityType entityType;

}
