package org.folio.dew.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemFormat {
  private static final Map<String, String> map = new LinkedHashMap<>();

  static {
    map.put("id", "Item id");
    map.put("version", "Version");
    map.put("hrid", "Item HRID");
    map.put("holdingsRecordId", "Holdings Record Id");
    map.put("formerIds", "Former Ids");
    map.put("discoverySuppress", "Discovery Suppress");
    map.put("title", "Title");
    map.put("contributorNames", "Contributor Names");
    map.put("callNumber", "Call Number");
    map.put("barcode", "Barcode");
    map.put("effectiveShelvingOrder", "Effective Shelving Order");
    map.put("accessionNumber", "Accession Number");
    map.put("itemLevelCallNumber", "Item Level Call Number");
    map.put("itemLevelCallNumberPrefix", "Item Level Call Number Prefix");
    map.put("itemLevelCallNumberSuffix", "Item Level Call Number Suffix");
    map.put("itemLevelCallNumberType", "Item Level Call Number Type");
    map.put("effectiveCallNumberComponents", "Effective Call Number Components");
    map.put("volume", "Volume");
    map.put("enumeration", "Enumeration");
    map.put("chronology", "Chronology");
    map.put("yearCaption", "Year Caption");
    map.put("itemIdentifier", "Item Identifier");
    map.put("copyNumber", "Copy Number");
    map.put("numberOfPieces", "Number Of Pieces");
    map.put("descriptionOfPieces", "Description Of Pieces");
    map.put("numberOfMissingPieces", "Number Of Missing Pieces");
    map.put("missingPieces", "Missing Pieces");
    map.put("missingPiecesDate", "Missing Pieces Date");
    map.put("itemDamagedStatus", "Item Damaged Status");
    map.put("itemDamagedStatusDate", "Item Damaged Status Date");
    map.put("administrativeNotes", "Administrative Notes");
    map.put("notes", "Notes");
    map.put("circulationNotes", "Circulation Notes");
    map.put("status", "Status");
    map.put("materialType", "Material Type");
    map.put("isBoundWith", "Is Bound With");
    map.put("boundWithTitles", "Bound With Titles");
    map.put("permanentLoanType", "Permanent Loan Type");
    map.put("temporaryLoanType", "Temporary Loan Type");
    map.put("permanentLocation", "Permanent Location");
    map.put("temporaryLocation", "Temporary Location");
    map.put("effectiveLocation", "Effective Location");
    map.put("electronicAccess", "Electronic Access");
    map.put("inTransitDestinationServicePoint", "In Transit Destination Service Point");
    map.put("statisticalCodes", "Statistical Codes");
    map.put("purchaseOrderLineIdentifier", "Purchase Order LineIdentifier");
    map.put("tags", "Tags");
    map.put("lastCheckIn", "Last CheckIn");
  }

  private String id;
  private String version;
  private String hrid;
  private String holdingsRecordId;
  private String formerIds;
  private String discoverySuppress;
  private String title;
  private String contributorNames;
  private String callNumber;
  private String barcode;
  private String effectiveShelvingOrder;
  private String accessionNumber;
  private String itemLevelCallNumber;
  private String itemLevelCallNumberPrefix;
  private String itemLevelCallNumberSuffix;
  private String itemLevelCallNumberType;
  private String effectiveCallNumberComponents;
  private String volume;
  private String enumeration;
  private String chronology;
  private String yearCaption;
  private String itemIdentifier;
  private String copyNumber;
  private String numberOfPieces;
  private String descriptionOfPieces;
  private String numberOfMissingPieces;
  private String missingPieces;
  private String missingPiecesDate;
  private String itemDamagedStatus;
  private String itemDamagedStatusDate;
  private String administrativeNotes;
  private String notes;
  private String circulationNotes;
  private String status;
  private String materialType;
  private String isBoundWith;
  private String boundWithTitles;
  private String permanentLoanType;
  private String temporaryLoanType;
  private String permanentLocation;
  private String temporaryLocation;
  private String effectiveLocation;
  private String electronicAccess;
  private String inTransitDestinationServicePoint;
  private String statisticalCodes;
  private String purchaseOrderLineIdentifier;
  private String tags;
  private String lastCheckIn;

  public static String getItemColumnHeaders() {
    return String.join(",", map.values());
  }

  public static String[] getItemFieldsArray() {
    return map.keySet().toArray(new String[0]);
  }
}

