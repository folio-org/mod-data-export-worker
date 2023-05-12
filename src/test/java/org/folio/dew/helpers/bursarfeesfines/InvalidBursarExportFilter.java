package org.folio.dew.helpers.bursarfeesfines;

import org.folio.dew.domain.dto.BursarExportFilter;

public class InvalidBursarExportFilter implements BursarExportFilter {
  @Override
  public String getType() {
    return null;
  }
}
