package org.folio.dew.domain.dto.authoritycontrol;

import java.time.OffsetDateTime;
import java.util.List;

public interface DataStatCollectionDTO {

  List<? extends DataStatDTO> getStats();

  OffsetDateTime getNext();
}
