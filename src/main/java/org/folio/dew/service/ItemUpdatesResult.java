package org.folio.dew.service;

import lombok.Getter;
import lombok.Setter;
import org.folio.dew.domain.dto.ItemFormat;

import java.util.List;

@Setter
@Getter
public class ItemUpdatesResult {
  private int total;
  private List<ItemFormat> updated;
}
