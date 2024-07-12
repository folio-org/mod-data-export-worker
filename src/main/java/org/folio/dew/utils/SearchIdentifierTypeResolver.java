package org.folio.dew.utils;

import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.BatchIdsDto;
import org.folio.dew.domain.dto.IdentifierType;

@UtilityClass
public class SearchIdentifierTypeResolver {

  public static String getSearchIdentifierType(IdentifierType identifierType) {
    return switch (identifierType) {
      case ID -> "id";
      case HRID -> "hrid";
      case BARCODE -> "barcode";
      case HOLDINGS_RECORD_ID -> "holdingsRecordId";
      case ACCESSION_NUMBER -> "accessionNumber";
      case FORMER_IDS ->  "formerIds";
      case INSTANCE_HRID -> "instanceHrid";
      case ITEM_BARCODE -> "itemBarcode";
      default -> throw new IllegalArgumentException("Identifier type doesn't supported");
    };
  }
}
