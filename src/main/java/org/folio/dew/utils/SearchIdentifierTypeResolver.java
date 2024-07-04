package org.folio.dew.utils;

import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.BatchIdsDto;
import org.folio.dew.domain.dto.IdentifierType;

@UtilityClass
public class SearchIdentifierTypeResolver {

  public static BatchIdsDto.IdentifierTypeEnum getSearchIdentifierType(IdentifierType identifierType) {
    return switch (identifierType) {
      case ID -> BatchIdsDto.IdentifierTypeEnum.ID;
      case HRID -> BatchIdsDto.IdentifierTypeEnum.HRID;
      case BARCODE -> BatchIdsDto.IdentifierTypeEnum.BARCODE;
      case HOLDINGS_RECORD_ID -> BatchIdsDto.IdentifierTypeEnum.HOLDINGSRECORDID;
      case ACCESSION_NUMBER -> BatchIdsDto.IdentifierTypeEnum.ACCESSIONNUMBER;
      case FORMER_IDS -> BatchIdsDto.IdentifierTypeEnum.FORMERIDS;
      case INSTANCE_HRID -> BatchIdsDto.IdentifierTypeEnum.INSTANCEHRID;
      case ITEM_BARCODE -> BatchIdsDto.IdentifierTypeEnum.ITEMBARCODE;
      default -> throw new IllegalArgumentException("Identifier type doesn't supported");
    };
  }
}
