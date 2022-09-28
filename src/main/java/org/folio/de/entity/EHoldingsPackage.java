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
  private String id;
  private Long jobExecutionId;
  private String ePackage;
  private String agreements;
  private String notes;
}
