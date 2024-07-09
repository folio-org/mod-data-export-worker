package org.folio.dew.domain.dto;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@With
@NoArgsConstructor
@AllArgsConstructor
public class UserFormat implements Formatable<org.folio.dew.domain.dto.User> {

  private org.folio.dew.domain.dto.User original;
  private static final Map<String, String> map = new LinkedHashMap<>();

  static {
    map.put("username", "User name");
    map.put("id", "User id");
    map.put("externalSystemId", "External system id");
    map.put("barcode", "Barcode");
    map.put("active", "Active");
    map.put("type", "Type");
    map.put("patronGroup", "Patron group");
    map.put("departments", "Departments");
    map.put("proxyFor", "Proxy for");
    map.put("lastName", "Last name");
    map.put("firstName", "First name");
    map.put("middleName", "Middle name");
    map.put("preferredFirstName", "Preferred first name");
    map.put("email", "Email");
    map.put("phone", "Phone");
    map.put("mobilePhone", "Mobile phone");
    map.put("dateOfBirth", "Date of birth");
    map.put("addresses", "Addresses");
    map.put("preferredContactTypeId", "Preferred contact type id");
    map.put("profilePictureLink", "Link to the profile picture");
    map.put("enrollmentDate", "Enrollment date");
    map.put("expirationDate", "Expiration date");
    map.put("tags", "Tags");
    map.put("customFields", "Custom fields");
    map.put("preferredEmailCommunication", "Preferred email communication");
  }

  @CsvBindByName(column = "User name")
  @CsvBindByPosition(position = 0)
  private String username;

  @CsvBindByName(column = "User id")
  @CsvBindByPosition(position = 1)
  private String id;

  @CsvBindByName(column = "External system id")
  @CsvBindByPosition(position = 2)
  private String externalSystemId;

  @CsvBindByName(column = "Barcode")
  @CsvBindByPosition(position = 3)
  private String barcode;

  @CsvBindByName(column = "Active")
  @CsvBindByPosition(position = 4)
  private String active;

  @CsvBindByName(column = "Type")
  @CsvBindByPosition(position = 5)
  private String type;

  @CsvBindByName(column = "Patron group")
  @CsvBindByPosition(position = 6)
  private String patronGroup;

  @CsvBindByName(column = "Departments")
  @CsvBindByPosition(position = 7)
  private String departments;

  @CsvBindByName(column = "Proxy for")
  @CsvBindByPosition(position = 8)
  private String proxyFor;

  @CsvBindByName(column = "Last name")
  @CsvBindByPosition(position = 9)
  private String lastName;

  @CsvBindByName(column = "First name")
  @CsvBindByPosition(position = 10)
  private String firstName;

  @CsvBindByName(column = "Middle name")
  @CsvBindByPosition(position = 11)
  private String middleName;

  @CsvBindByName(column = "Preferred first name")
  @CsvBindByPosition(position = 12)
  private String preferredFirstName;

  @CsvBindByName(column = "Email")
  @CsvBindByPosition(position = 13)
  private String email;

  @CsvBindByName(column = "Phone")
  @CsvBindByPosition(position = 14)
  private String phone;

  @CsvBindByName(column = "Mobile phone")
  @CsvBindByPosition(position = 15)
  private String mobilePhone;

  @CsvBindByName(column = "Date of birth")
  @CsvBindByPosition(position = 16)
  private String dateOfBirth;

  @CsvBindByName(column = "Addresses")
  @CsvBindByPosition(position = 17)
  private String addresses;

  @CsvBindByName(column = "Preferred contact type id")
  @CsvBindByPosition(position = 18)
  private String preferredContactTypeId;

  @CsvBindByName(column = "Link to the profile picture")
  @CsvBindByPosition(position = 19)
  private String profilePictureLink;

  @CsvBindByName(column = "Enrollment date")
  @CsvBindByPosition(position = 20)
  private String enrollmentDate;

  @CsvBindByName(column = "Expiration date")
  @CsvBindByPosition(position = 21)
  private String expirationDate;

  @CsvBindByName(column = "Tags")
  @CsvBindByPosition(position = 22)
  private String tags;

  @CsvBindByName(column = "Custom fields")
  @CsvBindByPosition(position = 23)
  private String customFields;

  @CsvBindByName(column = "Preferred email communication")
  @CsvBindByPosition(position = 24)
  private String preferredEmailCommunication;

  public static String getUserColumnHeaders() {
    return String.join(",", map.values());
  }

  public static String[] getUserFieldsArray() {
    return map.keySet().toArray(new String[0]);
  }

  public String getIdentifier(String identifierType) {
    try {
      switch (org.folio.dew.domain.dto.IdentifierType.fromValue(identifierType)) {
      case BARCODE:
        return barcode;
      case EXTERNAL_SYSTEM_ID:
        return externalSystemId;
      case USER_NAME:
        return username;
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

