package org.folio.de.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "e_holdings_resource")
public class EHoldingsResource {
  @Id
  private String id;
  private String name;
  private Long jobExecutionId;
  private String resourcesData;
  private String agreements;
  private String notes;
}
