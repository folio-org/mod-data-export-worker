package org.folio.dew.domain.dto.eholdings;

import java.util.List;

import lombok.Data;

import org.folio.dew.client.AgreementClient.Agreement;

@Data
public class EHoldingsPackage {
  private EProvider eProvider;
  private EPackage ePackage;
  private List<Agreement> agreements;
  private List<Note> notes;
}
