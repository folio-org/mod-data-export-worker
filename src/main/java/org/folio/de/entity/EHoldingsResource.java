package org.folio.de.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "e_holdings_resource")
public class EHoldingsResource {
  @Id
  @Column(name = "id")
  private String resourceId;
  @Column(name = "job_execution_id")
  private Long jobExecutionId;
  @Column(name = "resources_data")
  private String resourcesData;
  @Column(name = "agreements")
  private String agreements;
  @Column(name = "notes")
  private String notes;
}
