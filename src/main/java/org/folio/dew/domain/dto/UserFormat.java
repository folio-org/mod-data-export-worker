package org.folio.dew.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserFormat {
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
}
