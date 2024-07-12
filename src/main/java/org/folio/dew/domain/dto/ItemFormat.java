package org.folio.dew.domain.dto;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.stream.Collectors;

@Data
@Builder
@With
@NoArgsConstructor
@AllArgsConstructor
public class ItemFormat implements Formatable<org.folio.dew.domain.dto.Item> {

  private org.folio.dew.domain.dto.Item original;

  @CsvBindByName(column = "Item UUID")
  @CsvBindByPosition(position = 0)
  private String id;

  @CsvBindByName(column = "\"Instance (Title, Publisher, Publication date)\"")
  @CsvBindByPosition(position = 1)
  private String title;

  @CsvBindByName(column = "\"Holdings (Location, Call number)\"")
  @CsvBindByPosition(position = 2)
  private String holdingsData;

  @CsvBindByName(column = "Item effective location")
  @CsvBindByPosition(position = 3)
  private String effectiveLocation;

  @CsvBindByName(column = "Effective call number")
  @CsvBindByPosition(position = 4)
  private String effectiveCallNumberComponents;

  @CsvBindByName(column = "Suppress from discovery")
  @CsvBindByPosition(position = 5)
  private String discoverySuppress;

  @CsvBindByName(column = "Item HRID")
  @CsvBindByPosition(position = 6)
  private String hrid;

  @CsvBindByName(column = "Barcode")
  @CsvBindByPosition(position = 7)
  private String barcode;

  @CsvBindByName(column = "Accession number")
  @CsvBindByPosition(position = 8)
  private String accessionNumber;

  @CsvBindByName(column = "Item identifier")
  @CsvBindByPosition(position = 9)
  private String itemIdentifier;

  @CsvBindByName(column = "Former identifier")
  @CsvBindByPosition(position = 10)
  private String formerIds;

  @CsvBindByName(column = "Statistical codes")
  @CsvBindByPosition(position = 11)
  private String statisticalCodes;

  @CsvBindByName(column = "Administrative note")
  @CsvBindByPosition(position = 12)
  private String administrativeNotes;

  @CsvBindByName(column = "Material type")
  @CsvBindByPosition(position = 13)
  private String materialType;

  @CsvBindByName(column = "Copy number")
  @CsvBindByPosition(position = 14)
  private String copyNumber;

  @CsvBindByName(column = "Shelving order")
  @CsvBindByPosition(position = 15)
  private String effectiveShelvingOrder;

  @CsvBindByName(column = "Item level call number type")
  @CsvBindByPosition(position = 16)
  private String itemLevelCallNumberType;

  @CsvBindByName(column = "Item level call number prefix")
  @CsvBindByPosition(position = 17)
  private String itemLevelCallNumberPrefix;

  @CsvBindByName(column = "Item level call number")
  @CsvBindByPosition(position = 18)
  private String itemLevelCallNumber;

  @CsvBindByName(column = "Item level call number suffix")
  @CsvBindByPosition(position = 19)
  private String itemLevelCallNumberSuffix;

  @CsvBindByName(column = "Number of pieces")
  @CsvBindByPosition(position = 20)
  private String numberOfPieces;

  @CsvBindByName(column = "Description of pieces")
  @CsvBindByPosition(position = 21)
  private String descriptionOfPieces;

  @CsvBindByName(column = "Enumeration")
  @CsvBindByPosition(position = 22)
  private String enumeration;

  @CsvBindByName(column = "Chronology")
  @CsvBindByPosition(position = 23)
  private String chronology;

  @CsvBindByName(column = "Volume")
  @CsvBindByPosition(position = 24)
  private String volume;

  @CsvBindByName(column = "\"Year, caption\"")
  @CsvBindByPosition(position = 25)
  private String yearCaption;

  @CsvBindByName(column = "Number of missing pieces")
  @CsvBindByPosition(position = 26)
  private String numberOfMissingPieces;

  @CsvBindByName(column = "Missing pieces")
  @CsvBindByPosition(position = 27)
  private String missingPieces;

  @CsvBindByName(column = "Missing pieces date")
  @CsvBindByPosition(position = 28)
  private String missingPiecesDate;

  @CsvBindByName(column = "Item damaged status")
  @CsvBindByPosition(position = 29)
  private String itemDamagedStatus;

  @CsvBindByName(column = "Item damaged status date")
  @CsvBindByPosition(position = 30)
  private String itemDamagedStatusDate;

  @CsvBindByName(column = "Notes")
  @CsvBindByPosition(position = 31)
  private String notes;

  @CsvBindByName(column = "Permanent loan type")
  @CsvBindByPosition(position = 32)
  private String permanentLoanType;

  @CsvBindByName(column = "Temporary loan type")
  @CsvBindByPosition(position = 33)
  private String temporaryLoanType;

  @CsvBindByName(column = "Status")
  @CsvBindByPosition(position = 34)
  private String status;

  @CsvBindByName(column = "Check in note")
  @CsvBindByPosition(position = 35)
  private String checkInNotes;

  @CsvBindByName(column = "Check out note")
  @CsvBindByPosition(position = 36)
  private String checkOutNotes;

  @CsvBindByName(column = "Item permanent location")
  @CsvBindByPosition(position = 37)
  private String permanentLocation;

  @CsvBindByName(column = "Item temporary location")
  @CsvBindByPosition(position = 38)
  private String temporaryLocation;

  @CsvBindByName(column = "Electronic access")
  @CsvBindByPosition(position = 39)
  private String electronicAccess;

  @CsvBindByName(column = "Is bound with")
  @CsvBindByPosition(position = 40)
  private String isBoundWith;

  @CsvBindByName(column = "Bound with titles")
  @CsvBindByPosition(position = 41)
  private String boundWithTitles;

  @CsvBindByName(column = "Tags")
  @CsvBindByPosition(position = 42)
  private String tags;

  @CsvBindByName(column = "Holdings UUID")
  @CsvBindByPosition(position = 43)
  private String holdingsRecordId;

  public static String[] getItemFieldsArray() {
    return FieldUtils.getFieldsListWithAnnotation(ItemFormat.class, CsvBindByName.class).stream()
      .map(Field::getName)
      .toArray(String[]::new);
  }

  public static String getItemColumnHeaders() {
    return FieldUtils.getFieldsListWithAnnotation(ItemFormat.class, CsvBindByName.class).stream()
      .map(field -> field.getAnnotation(CsvBindByName.class).column())
      .collect(Collectors.joining(","));
  }

  public String getIdentifier(String identifierType) {
    try {
      switch (org.folio.dew.domain.dto.IdentifierType.fromValue(identifierType)) {
      case BARCODE:
        return barcode;
      case HOLDINGS_RECORD_ID:
        return holdingsRecordId;
      case HRID:
        return hrid;
      case FORMER_IDS:
        return formerIds;
      case ACCESSION_NUMBER:
        return accessionNumber;
      default:
        return id;
      }
    } catch (Exception e) {
      return id;
    }
  }

  @Override
  public boolean isInstanceFormat() {
    return false;
  }

  @Override
  public boolean isSourceMarc() {
    return false;
  }
}

