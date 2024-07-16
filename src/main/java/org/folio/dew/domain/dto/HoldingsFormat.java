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
public class HoldingsFormat implements Formatable<HoldingsRecord> {
  private HoldingsRecord original;
  private String tenantId;

  @CsvBindByName(column = "Holdings UUID")
  @CsvBindByPosition(position = 0)
  private String id;

  @CsvBindByName(column = "\"Instance (Title, Publisher, Publication date)\"")
  @CsvBindByPosition(position = 1)
  private String instanceTitle;

  @CsvBindByName(column = "Suppress from discovery")
  @CsvBindByPosition(position = 2)
  private String discoverySuppress;

  @CsvBindByName(column = "Holdings HRID")
  @CsvBindByPosition(position = 3)
  private String hrid;

  @CsvBindByName(column = "Source")
  @CsvBindByPosition(position = 4)
  private String source;

  @CsvBindByName(column = "Former holdings Id")
  @CsvBindByPosition(position = 5)
  private String formerIds;

  @CsvBindByName(column = "Holdings type")
  @CsvBindByPosition(position = 6)
  private String holdingsType;

  @CsvBindByName(column = "Statistical codes")
  @CsvBindByPosition(position = 7)
  private String statisticalCodes;

  @CsvBindByName(column = "Administrative note")
  @CsvBindByPosition(position = 8)
  private String administrativeNotes;

  @CsvBindByName(column = "Holdings permanent location")
  @CsvBindByPosition(position = 9)
  private String permanentLocation;

  @CsvBindByName(column = "Holdings temporary location")
  @CsvBindByPosition(position = 10)
  private String temporaryLocation;

  @CsvBindByName(column = "Shelving title")
  @CsvBindByPosition(position = 11)
  private String shelvingTitle;

  @CsvBindByName(column = "Holdings copy number")
  @CsvBindByPosition(position = 12)
  private String copyNumber;

  @CsvBindByName(column = "Holdings level call number type")
  @CsvBindByPosition(position = 13)
  private String callNumberType;

  @CsvBindByName(column = "Holdings level call number prefix")
  @CsvBindByPosition(position = 14)
  private String callNumberPrefix;

  @CsvBindByName(column = "Holdings level call number")
  @CsvBindByPosition(position = 15)
  private String callNumber;

  @CsvBindByName(column = "Holdings level call number suffix")
  @CsvBindByPosition(position = 16)
  private String callNumberSuffix;

  @CsvBindByName(column = "Number of items")
  @CsvBindByPosition(position = 17)
  private String numberOfItems;

  @CsvBindByName(column = "Holdings statement")
  @CsvBindByPosition(position = 18)
  private String holdingsStatements;

  @CsvBindByName(column = "Holdings statement for supplements")
  @CsvBindByPosition(position = 19)
  private String holdingsStatementsForSupplements;

  @CsvBindByName(column = "Holdings statement for indexes")
  @CsvBindByPosition(position = 20)
  private String holdingsStatementsForIndexes;

  @CsvBindByName(column = "ILL policy")
  @CsvBindByPosition(position = 21)
  private String illPolicy;

  @CsvBindByName(column = "Digitization policy")
  @CsvBindByPosition(position = 22)
  private String digitizationPolicy;

  @CsvBindByName(column = "Retention policy")
  @CsvBindByPosition(position = 23)
  private String retentionPolicy;

  @CsvBindByName(column = "Notes")
  @CsvBindByPosition(position = 24)
  private String notes;

  @CsvBindByName(column = "Electronic access")
  @CsvBindByPosition(position = 25)
  private String electronicAccess;

  @CsvBindByName(column = "Acquisition method")
  @CsvBindByPosition(position = 26)
  private String acquisitionMethod;

  @CsvBindByName(column = "Order format")
  @CsvBindByPosition(position = 27)
  private String acquisitionFormat;

  @CsvBindByName(column = "Receipt status")
  @CsvBindByPosition(position = 28)
  private String receiptStatus;

  @CsvBindByName(column = "Tags")
  @CsvBindByPosition(position = 29)
  private String tags;

  private String instanceHrid;
  private String itemBarcode;

  public static String[] getHoldingsFieldsArray() {
    return FieldUtils.getFieldsListWithAnnotation(HoldingsFormat.class, CsvBindByName.class).stream()
      .map(Field::getName)
      .toArray(String[]::new);
  }

  public static String getHoldingsColumnHeaders() {
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

  @Override
  public boolean isInstanceFormat() {
    return false;
  }

  @Override
  public boolean isSourceMarc() {
    return source.equals("MARC");
  }
}
