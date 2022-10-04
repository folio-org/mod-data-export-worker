package org.folio.dew.domain.dto.eholdings;

import java.util.List;

import org.folio.dew.client.AgreementClient;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EHoldingsResourceDTO {
  private ResourcesData resourcesData;
  private List<AgreementClient.Agreement> agreements;
  private List<Note> notes;
}
