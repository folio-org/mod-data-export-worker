package org.folio.dew.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.folio.dew.utils.Constants.UTF8_BOM;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemIdentifier {
  private String itemId;

  public void setItemId(String itemId) {
    if (itemId.startsWith(UTF8_BOM)) {
      itemId = itemId.substring(1);
    }
    this.itemId = itemId;
  }
}
