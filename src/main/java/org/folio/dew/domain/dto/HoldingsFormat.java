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
public class HoldingsFormat {
  @CsvBindByName(column = "Holdings record id")
  @CsvBindByPosition(position = 0)
  private String id;

  @CsvBindByName(column = "Version")
  @CsvBindByPosition(position = 1)
  private String version;

  @CsvBindByName(column = "HRID")
  @CsvBindByPosition(position = 2)
  private String hrid;

  @CsvBindByName(column = "Holdings type")
  @CsvBindByPosition(position = 3)
  private String holdingsType;

  @CsvBindByName(column = "Former ids")
  @CsvBindByPosition(position = 4)
  private String formerIds;

  @CsvBindByName(column = "Instance")
  @CsvBindByPosition(position = 5)
  private String instance;

  @CsvBindByName(column = "Permanent location")
  @CsvBindByPosition(position = 6)
  private String permanentLocation;

  @CsvBindByName(column = "Temporary location")
  @CsvBindByPosition(position = 7)
  private String temporaryLocation;

  @CsvBindByName(column = "Effective location")
  @CsvBindByPosition(position = 8)
  private String effectiveLocation;

  @CsvBindByName(column = "Electronic access")
  @CsvBindByPosition(position = 9)
  private String electronicAccess;

  @CsvBindByName(column = "Call number type")
  @CsvBindByPosition(position = 10)
  private String callNumberType;

  @CsvBindByName(column = "Call number prefix")
  @CsvBindByPosition(position = 11)
  private String callNumberPrefix;

  @CsvBindByName(column = "Call number")
  @CsvBindByPosition(position = 12)
  private String callNumber;

  @CsvBindByName(column = "Call number suffix")
  @CsvBindByPosition(position = 13)
  private String callNumberSuffix;

  @CsvBindByName(column = "Shelving title")
  @CsvBindByPosition(position = 14)
  private String shelvingTitle;

  @CsvBindByName(column = "Acquisition format")
  @CsvBindByPosition(position = 15)
  private String acquisitionFormat;

  @CsvBindByName(column = "Acquisition method")
  @CsvBindByPosition(position = 16)
  private String acquisitionMethod;

  @CsvBindByName(column = "Receipt status")
  @CsvBindByPosition(position = 17)
  private String receiptStatus;

  @CsvBindByName(column = "Administrative notes")
  @CsvBindByPosition(position = 18)
  private String administrativeNotes;

  @CsvBindByName(column = "Notes")
  @CsvBindByPosition(position = 19)
  private String notes;

  @CsvBindByName(column = "Ill policy")
  @CsvBindByPosition(position = 20)
  private String illPolicy;

  @CsvBindByName(column = "Retention policy")
  @CsvBindByPosition(position = 21)
  private String retentionPolicy;

  @CsvBindByName(column = "Digitization policy")
  @CsvBindByPosition(position = 22)
  private String digitizationPolicy;

  @CsvBindByName(column = "Holdings statements")
  @CsvBindByPosition(position = 23)
  private String holdingsStatements;

  @CsvBindByName(column = "Holdings statements for indexes")
  @CsvBindByPosition(position = 24)
  private String holdingsStatementsForIndexes;

  @CsvBindByName(column = "Holdings statements for supplements")
  @CsvBindByPosition(position = 25)
  private String holdingsStatementsForSupplements;

  @CsvBindByName(column = "Copy number")
  @CsvBindByPosition(position = 26)
  private String copyNumber;

  @CsvBindByName(column = "Number of items")
  @CsvBindByPosition(position = 27)
  private String numberOfItems;

  @CsvBindByName(column = "Receiving history")
  @CsvBindByPosition(position = 28)
  private String receivingHistory;

  @CsvBindByName(column = "Discovery suppress")
  @CsvBindByPosition(position = 29)
  private String discoverySuppress;

  @CsvBindByName(column = "Statistical codes")
  @CsvBindByPosition(position = 30)
  private String statisticalCodes;

  @CsvBindByName(column = "Tags")
  @CsvBindByPosition(position = 31)
  private String tags;

  @CsvBindByName(column = "Source")
  @CsvBindByPosition(position = 32)
  private String source;

  private String instanceHrid;
  private String itemBarcode;

  public static String[] getItemFieldsArray() {
    return FieldUtils.getFieldsListWithAnnotation(HoldingsFormat.class, CsvBindByName.class).stream()
      .map(Field::getName)
      .toArray(String[]::new);
  }

  public static String getItemColumnHeaders() {
    return FieldUtils.getFieldsListWithAnnotation(HoldingsFormat.class, CsvBindByName.class).stream()
      .map(field -> field.getAnnotation(CsvBindByName.class).column())
      .collect(Collectors.joining(","));
  }

  public String getIdentifier(String identifierType) {
    try {
      switch (IdentifierType.fromValue(identifierType)) {
      case HRID:
        return hrid;
      case INSTANCE_HRID:
        return instanceHrid;
      case ITEM_BARCODE:
        return itemBarcode;
      default:
        return id;
      }
    } catch (Exception e) {
      return id;
    }
  }
}
