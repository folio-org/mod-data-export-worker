package org.folio.dew.domain.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.ExportTypeSpecificParameters;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.JobStatus;
import org.folio.dew.domain.dto.Progress;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
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

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private ExportTypeSpecificParameters exportTypeSpecificParameters;

  @Enumerated(EnumType.STRING)
  private JobStatus status;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private List<String> files = null;

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

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private ExitStatus exitStatus;

  @Enumerated(EnumType.STRING)
  private IdentifierType identifierType;

  @Enumerated(EnumType.STRING)
  private EntityType entityType;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Progress progress;

}
