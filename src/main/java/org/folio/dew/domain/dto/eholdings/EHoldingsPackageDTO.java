package org.folio.dew.domain.dto.eholdings;

import java.util.List;

import org.folio.dew.client.AgreementClient.Agreement;

import lombok.Data;

@Data
public class EHoldingsPackageDTO {
  private EProvider eProvider;
  private EPackage ePackage;
  private List<Agreement> agreements;
  private List<Note> notes;
}
