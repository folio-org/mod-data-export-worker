package org.folio.dew.domain.dto.eholdings;

import java.util.List;

import lombok.Singular;
import org.folio.dew.client.AgreementClient;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EHoldingsResourceDTO {
  private ResourcesData resourcesData;
  @Singular private List<AgreementClient.Agreement> agreements;
  @Singular private List<Note> notes;
}
