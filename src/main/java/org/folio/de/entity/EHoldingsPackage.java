package org.folio.de.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "e_holdings_package")
public class EHoldingsPackage {
  @Id
  @Column(name = "id")
  private String id;
  @Column(name = "job_execution_id")
  private Long jobExecutionId;
  @Column(name = "e_package")
  private String ePackage;
  @Column(name = "agreements")
  private String agreements;
  @Column(name = "notes")
  private String notes;
}
