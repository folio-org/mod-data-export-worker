package org.folio.dew.domain.dto;


import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.folio.dew.domain.dto.IdentifierType;

import java.lang.reflect.Field;
import java.util.stream.Collectors;
@Data
@Builder
@With
@NoArgsConstructor
@AllArgsConstructor
public class InstanceFormat implements Formatable<org.folio.dew.domain.dto.Instance> {

  private org.folio.dew.domain.dto.Instance original;

  @CsvBindByName(column = "Instance UUID")
  @CsvBindByPosition(position = 0)
  private String id;

  @CsvBindByName(column = "Suppress from discovery")
  @CsvBindByPosition(position = 1)
  private String discoverySuppress;

  @CsvBindByName(column = "Staff suppress")
  @CsvBindByPosition(position = 2)
  private String staffSuppress;

  @CsvBindByName(column = "Previously held")
  @CsvBindByPosition(position = 3)
  private String previouslyHeld;

  @CsvBindByName(column = "Instance HRID")
  @CsvBindByPosition(position = 4)
  private String hrid;

  @CsvBindByName(column = "Source")
  @CsvBindByPosition(position = 5)
  private String source;

  @CsvBindByName(column = "Cataloged date")
  @CsvBindByPosition(position = 6)
  private String catalogedDate;

  @CsvBindByName(column = "Instance status term")
  @CsvBindByPosition(position = 7)
  private String statusId;

  @CsvBindByName(column = "Mode of issuance")
  @CsvBindByPosition(position = 8)
  private String modeOfIssuanceId;

  @CsvBindByName(column = "Administrative note")
  @CsvBindByPosition(position = 9)
  private String administrativeNotes;

  @CsvBindByName(column = "Resource title")
  @CsvBindByPosition(position = 10)
  private String title;

  @CsvBindByName(column = "Index title")
  @CsvBindByPosition(position = 11)
  private String indexTitle;

  @CsvBindByName(column = "Series statements")
  @CsvBindByPosition(position = 12)
  private String series;

  @CsvBindByName(column = "Contributors")
  @CsvBindByPosition(position = 13)
  private String contributors;

  @CsvBindByName(column = "Edition")
  @CsvBindByPosition(position = 14)
  private String editions;

  @CsvBindByName(column = "Physical description")
  @CsvBindByPosition(position = 15)
  private String physicalDescriptions;

  @CsvBindByName(column = "Resource type")
  @CsvBindByPosition(position = 16)
  private String instanceTypeId;

  @CsvBindByName(column = "Nature of content")
  @CsvBindByPosition(position = 17)
  private String natureOfContentTermIds;

  @CsvBindByName(column = "Formats")
  @CsvBindByPosition(position = 18)
  private String instanceFormatIds;

  @CsvBindByName(column = "Languages")
  @CsvBindByPosition(position = 19)
  private String languages;

  @CsvBindByName(column = "Publication frequency")
  @CsvBindByPosition(position = 20)
  private String publicationFrequency;

  @CsvBindByName(column = "Publication range")
  @CsvBindByPosition(position = 21)
  private String publicationRange;

  @CsvBindByName(column = "Notes")
  @CsvBindByPosition(position = 22)
  private String notes;

  private String isbn;
  private String issn;

  public static String[] getInstanceFieldsArray() {
    return FieldUtils.getFieldsListWithAnnotation(InstanceFormat.class, CsvBindByName.class).stream()
      .map(Field::getName)
      .toArray(String[]::new);
  }

  public static String getInstanceColumnHeaders() {
    return FieldUtils.getFieldsListWithAnnotation(InstanceFormat.class, CsvBindByName.class).stream()
      .map(field -> field.getAnnotation(CsvBindByName.class).column())
      .collect(Collectors.joining(","));
  }

  public String getIdentifier(String identifierType) {
    try {
      switch (IdentifierType.fromValue(identifierType)) {
        case HRID:
          return hrid;
        case ISBN:
          return isbn;
        case ISSN:
          return issn;
        default:
          return id;
      }
    } catch (Exception e) {
      return id;
    }
  }

  @Override
  public boolean isInstanceFormat() {
    return true;
  }

  @Override
  public boolean isSourceMarc() {
    return source.equals("MARC");
  }
}
