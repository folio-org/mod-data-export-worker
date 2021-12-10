package org.folio.dew.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class UserFormat {
  private static final Map<String, String> map = new LinkedHashMap<>();

  static {
    map.put("userName", "User name");
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
    map.put("enrollmentDate", "Enrollment date");
    map.put("expirationDate", "Expiration date");
    map.put("createdDate", "Created date");
    map.put("updatedDate", "Updated date");
    map.put("tags", "Tags");
    map.put("customFields", "Custom fields");
  }

  private String userName;
  private String id;
  private String externalSystemId;
  private String barcode;
  private String active;
  private String type;
  private String patronGroup;
  private String departments;
  private String proxyFor;

  private String lastName;
  private String firstName;
  private String middleName;
  private String preferredFirstName;
  private String email;
  private String phone;
  private String mobilePhone;
  private String dateOfBirth;
  private String addresses;
  private String preferredContactTypeId;

  private String enrollmentDate;
  private String expirationDate;
  private String createdDate;
  private String updatedDate;
  private String tags;
  private String customFields;

  public static String getUserColumnHeaders() {
    return String.join(",", map.values());
  }

  public static String[] getUserFieldsArray() {
    return map.keySet().toArray(new String[0]);
  }
}
