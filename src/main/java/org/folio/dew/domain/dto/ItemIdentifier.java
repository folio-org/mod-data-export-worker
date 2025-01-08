package org.folio.dew.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.dew.utils.CsvHelper;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemIdentifier {
  private String itemId;

  public void setItemId(String itemId) {
    this.itemId = CsvHelper.clearBomSymbol(itemId);
  }
}
