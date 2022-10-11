package org.folio.de.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  @JsonProperty("ePackage")
  private String ePackage;
  @JsonProperty("eProvider")
  private String eProvider;
  private String agreements;
  private String notes;
}
