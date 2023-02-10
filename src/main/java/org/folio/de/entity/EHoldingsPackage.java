package org.folio.de.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@Entity
@Table(name = "e_holdings_package")
@IdClass(EHoldingsPackage.PackageId.class)
public class EHoldingsPackage {
  @Id
  private String id;
  @Id
  private Long jobExecutionId;
  @JsonProperty("ePackage")
  private String ePackage;
  @JsonProperty("eProvider")
  private String eProvider;
  private String agreements;
  private String notes;

  @Data
  public static class PackageId implements Serializable {
    private String id;
    private Long jobExecutionId;
  }
}
