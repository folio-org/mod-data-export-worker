package org.folio.de.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.IdentifierType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.springframework.batch.core.job.parameters.JobParameters;

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
  @JdbcTypeCode(SqlTypes.JSON)
  private JobParameters jobParameters;
  @Enumerated(EnumType.STRING)
  private IdentifierType identifierType;
  @Enumerated(EnumType.STRING)
  private EntityType entityType;

}
