package org.folio.dew.domain.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.dew.utils.CsvHelper;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ItemIdentifier implements Serializable {
  private String itemId;

  public ItemIdentifier(String itemId) {
    setItemId(itemId);
  }

  public void setItemId(String itemId) {
    this.itemId = CsvHelper.clearBomSymbol(itemId);
  }
}
