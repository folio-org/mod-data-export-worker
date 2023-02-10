package org.folio.de.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "e_holdings_resource")
@IdClass(EHoldingsResource.ResourceId.class)
public class EHoldingsResource {
  @Id
  private String id;
  private String name;
  @Id
  private Long jobExecutionId;
  private String resourcesData;
  private String agreements;
  private String notes;

  @Data
  public static class ResourceId implements Serializable {
    private String id;
    private Long jobExecutionId;
  }
}
