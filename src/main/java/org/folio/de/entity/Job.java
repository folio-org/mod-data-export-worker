package org.folio.de.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.ExportTypeSpecificParameters;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.JobStatus;
import org.folio.dew.domain.dto.Progress;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class Job {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.folio.dew.repository.generator.CustomUUIDGenerator")
  @Column(updatable = false, nullable = false)
  private UUID id;

  private String name;

  private String description;

  private String source;

  private Boolean isSystemSource;

  @Enumerated(EnumType.STRING)
  private ExportType type;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private ExportTypeSpecificParameters exportTypeSpecificParameters;

  @Enumerated(EnumType.STRING)
  private JobStatus status;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private List<String> files = null;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private List<String> fileNames = null;

  private Date startTime;

  private Date endTime;

  private Date createdDate;

  private UUID createdByUserId;

  private String createdByUsername;

  private Date updatedDate;

  private UUID updatedByUserId;

  private String updatedByUsername;

  private String outputFormat;

  private String errorDetails;

  @Enumerated(EnumType.STRING)
  private BatchStatus batchStatus;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private ExitStatus exitStatus;

  @Enumerated(EnumType.STRING)
  private IdentifierType identifierType;

  @Enumerated(EnumType.STRING)
  private EntityType entityType;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Progress progress;

}
