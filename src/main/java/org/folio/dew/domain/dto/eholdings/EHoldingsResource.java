package org.folio.dew.domain.dto.eholdings;

import java.util.List;

import lombok.Builder;
import lombok.Data;

import org.folio.dew.client.AgreementClient;

@Data
@Builder
public class EHoldingsResource {
  private ResourcesData resourcesData;
  private List<AgreementClient.Agreement> agreements;
  private List<Note> notes;
}
