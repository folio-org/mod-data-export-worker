package org.folio.dew.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.Address;
import org.folio.dew.domain.dto.Department;
import org.folio.dew.domain.dto.Personal;
import org.folio.dew.domain.dto.ProxyFor;
import org.folio.dew.domain.dto.Tags;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.domain.dto.UserGroupCollection;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import static java.time.ZoneOffset.UTC;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.DATE_TIME_PATTERN;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.dew.utils.Constants.KEY_VALUE_DELIMITER;
import static org.folio.dew.utils.Constants.LINE_BREAK;
import static org.folio.dew.utils.Constants.LINE_BREAK_REPLACEMENT;

@Component
@RequiredArgsConstructor
public class BulkEditParseService {

  private final UserReferenceService userReferenceService;

  private static final int ADDRESS_ID = 0;
  private static final int ADDRESS_COUNTRY_ID = 1;
  private static final int ADDRESS_LINE_1 = 2;
  private static final int ADDRESS_LINE_2 = 3;
  private static final int ADDRESS_CITY = 4;
  private static final int ADDRESS_REGION = 5;
  private static final int ADDRESS_POSTAL_CODE = 6;
  private static final int ADDRESS_PRIMARY_ADDRESS = 7;
  private static final int ADDRESS_TYPE = 8;

  private static final int CF_KEY_INDEX = 0;
  private static final int CF_VALUE_INDEX = 1;

  private static final String START_ARRAY = "[";
  private static final String END_ARRAY = "]";

  public User mapUserFormatToUser(UserFormat userFormat) {
    User user = new User();
    populateUserFields(user, userFormat);
    return user;
  }

  private void populateUserFields(User user, UserFormat userFormat) {
    user.setId(userFormat.getId());
    user.setUsername(userFormat.getUserName());
    user.setExternalSystemId(isBlank(userFormat.getExternalSystemId()) ? null : userFormat.getExternalSystemId());
    user.setBarcode(isBlank(userFormat.getBarcode()) ? null : userFormat.getBarcode());
    user.setActive(getIsActive(userFormat));
    user.setType(userFormat.getType());
    user.setPatronGroup(getPatronGroupId(userFormat));
    user.setDepartments(getUserDepartments(userFormat));
    user.setProxyFor(getProxyFor(userFormat));
    user.setPersonal(getUserPersonalInfo(userFormat));
    user.setEnrollmentDate(getDate(userFormat.getEnrollmentDate()));
    user.setExpirationDate(getDate(userFormat.getExpirationDate()));
    user.setCreatedDate(getDate(userFormat.getCreatedDate()));
    user.setUpdatedDate(getDate(userFormat.getUpdatedDate()));
    user.setTags(getTags(userFormat));
    user.setCustomFields(getCustomFields(userFormat));
  }

  private boolean getIsActive(UserFormat userFormat) {
    String value = userFormat.getActive();
    if (value.matches("true") || value.matches("false")) {
      return Boolean.parseBoolean(value);
    }
    //TODO in MODBULKED-14 save error that filed has a wrong value instead of returning false
    return false;
  }

  private String getPatronGroupId(UserFormat userFormat) {
    if (isNotEmpty(userFormat.getPatronGroup())) {
      UserGroupCollection userGroup = userReferenceService.getUserGroupByGroupName(userFormat.getPatronGroup());
      if (!userGroup.getUsergroups().isEmpty()) {
        return userGroup.getUsergroups().iterator().next().getId();
      }
    }
    return null;
  }

  private List<UUID> getUserDepartments(UserFormat userFormat) {
    String[] departmentNames = userFormat.getDepartments().split(ARRAY_DELIMITER);
    if (departmentNames.length > 0) {
      return Arrays.stream(departmentNames).parallel()
        .filter(StringUtils::isNotEmpty)
        .map(userReferenceService::getDepartmentByName)
        .flatMap(departmentCollection -> departmentCollection.getDepartments().stream())
        .map(Department::getId)
        .map(UUID::fromString)
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private List<String> getProxyFor(UserFormat userFormat) {
    String[] proxyUserNames = userFormat.getProxyFor().split(ARRAY_DELIMITER);
    if (proxyUserNames.length > 0) {
      return Arrays.stream(proxyUserNames)
        .parallel()
        .filter(StringUtils::isNotEmpty)
        .map(userReferenceService::getUserByName)
        .flatMap(userCollection -> userCollection.getUsers().stream())
        .map(User::getId)
        .filter(StringUtils::isNotEmpty)
        .map(userReferenceService::getProxyForByProxyUserId)
        .flatMap(proxyForCollection -> proxyForCollection.getProxiesFor().stream())
        .map(ProxyFor::getId)
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private Personal getUserPersonalInfo(UserFormat userFormat) {
    Personal personal = new Personal();
    personal.setLastName(userFormat.getLastName());
    personal.setFirstName(userFormat.getFirstName());
    personal.setMiddleName(userFormat.getMiddleName());
    personal.setPreferredFirstName(userFormat.getPreferredFirstName());
    personal.setEmail(userFormat.getEmail());
    personal.setPhone(userFormat.getPhone());
    personal.setMobilePhone(userFormat.getMobilePhone());
    personal.setDateOfBirth(getDate(userFormat.getDateOfBirth()));
    personal.setAddresses(getUserAddresses(userFormat));
    personal.setPreferredContactTypeId(userFormat.getPreferredContactTypeId());
    return personal;
  }

  private Date getDate(String date) {
    if (isNotEmpty(date)) {
      LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
      return Date.from(localDateTime.atZone(UTC).toInstant());
    }
    return null;
  }

  private List<Address> getUserAddresses(UserFormat userFormat) {
    String[] addresses = userFormat.getAddresses().split(ITEM_DELIMITER_PATTERN);
    if (addresses.length > 0) {
      return Arrays.stream(addresses)
        .parallel()
        .filter(StringUtils::isNotEmpty)
        .map(this::getAddressFromString)
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private Address getAddressFromString(String stringAddress) {
    Address address = new Address();
    List<String> addressFields = Arrays.asList(stringAddress.split(ARRAY_DELIMITER));
    address.setId(addressFields.get(ADDRESS_ID));
    address.setCountryId(addressFields.get(ADDRESS_COUNTRY_ID));
    address.setAddressLine1(addressFields.get(ADDRESS_LINE_1));
    address.setAddressLine2(addressFields.get(ADDRESS_LINE_2));
    address.setCity(addressFields.get(ADDRESS_CITY));
    address.setRegion(addressFields.get(ADDRESS_REGION));
    address.setPostalCode(addressFields.get(ADDRESS_POSTAL_CODE));
    address.setPrimaryAddress(Boolean.valueOf(addressFields.get(ADDRESS_PRIMARY_ADDRESS)));
    //avoid IndexOutOfBoundsException if address type id wasn't set
    if (addressFields.size() == 9) {
      String id = userReferenceService.getAddressTypeByDesc(addressFields.get(ADDRESS_TYPE)).getAddressTypes().iterator().next().getId();
      address.setAddressTypeId(id);
    }
    return address;
  }

  private Tags getTags(UserFormat userFormat) {
    if (isNotEmpty(userFormat.getTags())) {
      Tags tags = new Tags();
      List<String> tagList = Arrays.asList(userFormat.getTags().split(ARRAY_DELIMITER));
      return tags.tagList(tagList);
    }
    return null;
  }

  private Map<String, Object> getCustomFields(UserFormat userFormat) {
    if (isNotEmpty(userFormat.getCustomFields())) {
      Map<String, Object> customFields = new HashMap<>();
      String[] customFieldsArray = userFormat.getCustomFields().split(ITEM_DELIMITER_PATTERN);
      Arrays.stream(customFieldsArray)
        .forEach(customField -> {
          List<String> customFieldKeyValue = Arrays.asList(customField.split(KEY_VALUE_DELIMITER));
          customFields.put(customFieldKeyValue.get(CF_KEY_INDEX), restoreCustomFieldValue(customFieldKeyValue.get(CF_VALUE_INDEX)));
        });
      return customFields;
    }
    return Collections.emptyMap();
  }

  private Object restoreCustomFieldValue(String s) {
    s = s.replace(LINE_BREAK_REPLACEMENT, LINE_BREAK);
    return s.startsWith(START_ARRAY) && s.endsWith(END_ARRAY) ? Arrays.asList(s.substring(1, s.length() - 1).split(", ")) : s;
  }

}
