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

  @CsvBindByName(column = "Item id")
  @CsvBindByPosition(position = 0)
  private String id;

  @CsvBindByName(column = "Version")
  @CsvBindByPosition(position = 1)
  private String version;

  @CsvBindByName(column = "Item HRID")
  @CsvBindByPosition(position = 2)
  private String hrid;

  @CsvBindByName(column = "Holdings Record Id")
  @CsvBindByPosition(position = 3)
  private String holdingsRecordId;

  @CsvBindByName(column = "Former Ids")
  @CsvBindByPosition(position = 4)
  private String formerIds;

  @CsvBindByName(column = "Discovery Suppress")
  @CsvBindByPosition(position = 5)
  private String discoverySuppress;

  @CsvBindByName(column = "Title")
  @CsvBindByPosition(position = 6)
  private String title;

  @CsvBindByName(column = "\"Holdings (Location, Call number)\"")
  @CsvBindByPosition(position = 7)
  private String holdingsData;

  @CsvBindByName(column = "Contributor Names")
  @CsvBindByPosition(position = 8)
  private String contributorNames;

  @CsvBindByName(column = "Call Number")
  @CsvBindByPosition(position = 9)
  private String callNumber;

  @CsvBindByName(column = "Barcode")
  @CsvBindByPosition(position = 10)
  private String barcode;

  @CsvBindByName(column = "Effective Shelving Order")
  @CsvBindByPosition(position = 11)
  private String effectiveShelvingOrder;

  @CsvBindByName(column = "Accession Number")
  @CsvBindByPosition(position = 12)
  private String accessionNumber;

  @CsvBindByName(column = "Item Level Call Number")
  @CsvBindByPosition(position = 13)
  private String itemLevelCallNumber;

  @CsvBindByName(column = "Item Level Call Number Prefix")
  @CsvBindByPosition(position = 14)
  private String itemLevelCallNumberPrefix;

  @CsvBindByName(column = "Item Level Call Number Suffix")
  @CsvBindByPosition(position = 15)
  private String itemLevelCallNumberSuffix;

  @CsvBindByName(column = "Item Level Call Number Type")
  @CsvBindByPosition(position = 16)
  private String itemLevelCallNumberType;

  @CsvBindByName(column = "Effective Call Number Components")
  @CsvBindByPosition(position = 17)
  private String effectiveCallNumberComponents;

  @CsvBindByName(column = "Volume")
  @CsvBindByPosition(position = 18)
  private String volume;

  @CsvBindByName(column = "Enumeration")
  @CsvBindByPosition(position = 19)
  private String enumeration;

  @CsvBindByName(column = "Chronology")
  @CsvBindByPosition(position = 20)
  private String chronology;

  @CsvBindByName(column = "Year Caption")
  @CsvBindByPosition(position = 21)
  private String yearCaption;

  @CsvBindByName(column = "Item Identifier")
  @CsvBindByPosition(position = 22)
  private String itemIdentifier;

  @CsvBindByName(column = "Copy Number")
  @CsvBindByPosition(position = 23)
  private String copyNumber;

  @CsvBindByName(column = "Number Of Pieces")
  @CsvBindByPosition(position = 24)
  private String numberOfPieces;

  @CsvBindByName(column = "Description Of Pieces")
  @CsvBindByPosition(position = 25)
  private String descriptionOfPieces;

  @CsvBindByName(column = "Number Of Missing Pieces")
  @CsvBindByPosition(position = 26)
  private String numberOfMissingPieces;

  @CsvBindByName(column = "Missing Pieces")
  @CsvBindByPosition(position = 27)
  private String missingPieces;

  @CsvBindByName(column = "Missing Pieces Date")
  @CsvBindByPosition(position = 28)
  private String missingPiecesDate;

  @CsvBindByName(column = "Item Damaged Status")
  @CsvBindByPosition(position = 29)
  private String itemDamagedStatus;

  @CsvBindByName(column = "Item Damaged Status Date")
  @CsvBindByPosition(position = 30)
  private String itemDamagedStatusDate;

  @CsvBindByName(column = "Administrative note")
  @CsvBindByPosition(position = 31)
  private String administrativeNotes;

  @CsvBindByName(column = "Notes")
  @CsvBindByPosition(position = 32)
  private String notes;

  @CsvBindByName(column = "Check In Notes")
  @CsvBindByPosition(position = 33)
  private String checkInNotes;

  @CsvBindByName(column = "Check Out Notes")
  @CsvBindByPosition(position = 34)
  private String checkOutNotes;

  @CsvBindByName(column = "Status")
  @CsvBindByPosition(position = 35)
  private String status;

  @CsvBindByName(column = "Material Type")
  @CsvBindByPosition(position = 36)
  private String materialType;

  @CsvBindByName(column = "Is Bound With")
  @CsvBindByPosition(position = 37)
  private String isBoundWith;

  @CsvBindByName(column = "Bound With Titles")
  @CsvBindByPosition(position = 38)
  private String boundWithTitles;

  @CsvBindByName(column = "Permanent Loan Type")
  @CsvBindByPosition(position = 39)
  private String permanentLoanType;

  @CsvBindByName(column = "Temporary Loan Type")
  @CsvBindByPosition(position = 40)
  private String temporaryLoanType;

  @CsvBindByName(column = "Permanent Location")
  @CsvBindByPosition(position = 41)
  private String permanentLocation;

  @CsvBindByName(column = "Temporary Location")
  @CsvBindByPosition(position = 42)
  private String temporaryLocation;

  @CsvBindByName(column = "Effective Location")
  @CsvBindByPosition(position = 43)
  private String effectiveLocation;

  @CsvBindByName(column = "Electronic Access")
  @CsvBindByPosition(position = 44)
  private String electronicAccess;

  @CsvBindByName(column = "In Transit Destination Service Point")
  @CsvBindByPosition(position = 45)
  private String inTransitDestinationServicePoint;

  @CsvBindByName(column = "Statistical Codes")
  @CsvBindByPosition(position = 46)
  private String statisticalCodes;

  @CsvBindByName(column = "Purchase Order LineIdentifier")
  @CsvBindByPosition(position = 47)
  private String purchaseOrderLineIdentifier;

  @CsvBindByName(column = "Tags")
  @CsvBindByPosition(position = 48)
  private String tags;

  @CsvBindByName(column = "Last CheckIn")
  @CsvBindByPosition(position = 49)
  private String lastCheckIn;

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

