package org.folio.dew.domain.dto.eholdings;

import java.util.List;

import lombok.Builder;
import lombok.Singular;
import org.folio.dew.client.AgreementClient.Agreement;

import lombok.Data;

@Data
@Builder
public class EHoldingsPackageDTO {
  private EProvider eProvider;
  private EPackage ePackage;
  @Singular private List<Agreement> agreements;
  @Singular private List<Note> notes;
}
