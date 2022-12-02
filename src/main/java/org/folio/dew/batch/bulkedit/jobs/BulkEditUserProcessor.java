package org.folio.dew.batch.bulkedit.jobs;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;
import static org.folio.dew.utils.Constants.KEY_VALUE_DELIMITER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.dto.*;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.UserReferenceService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditUserProcessor implements ItemProcessor<User, UserFormat> {
  private final UserReferenceService userReferenceService;
  private final BulkEditProcessingErrorsService errorsService;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;

  @Override
  public UserFormat process(User user) {
    var errorServiceArgs = new ErrorServiceArgs(jobId, getIdentifier(user, identifierType), FilenameUtils.getName(fileName));
    return UserFormat.builder()
      .username(user.getUsername())
      .id(user.getId())
      .externalSystemId(user.getExternalSystemId())
      .barcode(user.getBarcode())
      .active((isNull(user.getActive()) ? Boolean.FALSE : user.getActive()).toString())
      .type(user.getType())
      .patronGroup(userReferenceService.getPatronGroupNameById(user.getPatronGroup(), errorServiceArgs))
      .departments(fetchDepartments(user, errorServiceArgs))
      .proxyFor(isEmpty(user.getProxyFor()) ? EMPTY : String.join(ARRAY_DELIMITER, user.getProxyFor()))
      .lastName(user.getPersonal().getLastName())
      .firstName(user.getPersonal().getFirstName())
      .middleName(user.getPersonal().getMiddleName())
      .preferredFirstName(user.getPersonal().getPreferredFirstName())
      .email(user.getPersonal().getEmail())
      .phone(user.getPersonal().getPhone())
      .mobilePhone(user.getPersonal().getMobilePhone())
      .dateOfBirth(dateToString(user.getPersonal().getDateOfBirth()))
      .addresses(addressesToString(user.getPersonal().getAddresses(), errorServiceArgs))
      .preferredContactTypeId(isNull(user.getPersonal().getPreferredContactTypeId()) ? EMPTY : user.getPersonal().getPreferredContactTypeId())
      .enrollmentDate(dateToString(user.getEnrollmentDate()))
      .expirationDate(dateToString(user.getExpirationDate()))
      .createdDate(dateToString(user.getCreatedDate()))
      .updatedDate(dateToString(user.getUpdatedDate()))
      .tags(nonNull(user.getTags()) ? String.join(ARRAY_DELIMITER, user.getTags().getTagList()) : EMPTY)
      .customFields(nonNull(user.getCustomFields()) ? customFieldsToString(user.getCustomFields()) : EMPTY)
      .build();
  }

  private String fetchDepartments(User user, ErrorServiceArgs args) {
    if (nonNull(user.getDepartments())) {
      return user.getDepartments().stream()
        .map(id -> userReferenceService.getDepartmentNameById(id.toString(), args))
        .collect(Collectors.joining(ARRAY_DELIMITER));
    }
    return EMPTY;
  }

  private String addressesToString(List<Address> addresses, ErrorServiceArgs args) {
    if (nonNull(addresses)) {
      return addresses.stream()
        .map(address -> addressToString(address, args))
        .collect(Collectors.joining(ITEM_DELIMITER));
    }
    return EMPTY;
  }

  private String addressToString(Address address, ErrorServiceArgs args) {
    List<String> addressData = new ArrayList<>();
    addressData.add(ofNullable(address.getId()).orElse(EMPTY));
    addressData.add(ofNullable(address.getCountryId()).orElse(EMPTY));
    addressData.add(ofNullable(address.getAddressLine1()).orElse(EMPTY));
    addressData.add(ofNullable(address.getAddressLine2()).orElse(EMPTY));
    addressData.add(ofNullable(address.getCity()).orElse(EMPTY));
    addressData.add(ofNullable(address.getRegion()).orElse(EMPTY));
    addressData.add(ofNullable(address.getPostalCode()).orElse(EMPTY));
    addressData.add(nonNull(address.getPrimaryAddress()) ? address.getPrimaryAddress().toString() : EMPTY);
    addressData.add(userReferenceService.getAddressTypeDescById(address.getAddressTypeId(), args));
    return String.join(ARRAY_DELIMITER, addressData);
  }

  private String customFieldsToString(Map<String, Object> map) {
    return map.entrySet().stream()
      .map(this::customFieldToString)
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String customFieldToString(Map.Entry<String, Object> entry) {
    var customField = userReferenceService.getCustomFieldByRefId(entry.getKey());
    switch (customField.getType()) {
    case TEXTBOX_LONG:
    case TEXTBOX_SHORT:
    case SINGLE_CHECKBOX:
      return customField.getName() + KEY_VALUE_DELIMITER + entry.getValue();
    case SINGLE_SELECT_DROPDOWN:
    case RADIO_BUTTON:
      return customField.getName() + KEY_VALUE_DELIMITER + extractValueById(customField, entry.getValue().toString());
    case MULTI_SELECT_DROPDOWN:
      var values = (ArrayList) entry.getValue();
      return customField.getName() + KEY_VALUE_DELIMITER + values.stream()
        .map(v -> extractValueById(customField, v.toString()))
        .collect(Collectors.joining(ARRAY_DELIMITER));
    default:
      throw new BulkEditException("Invalid custom field: " + entry);
    }
  }

  private String extractValueById(CustomField customField, String id) {
    var optionalValue = customField.getSelectField().getOptions().getValues().stream()
      .filter(selectFieldOption -> Objects.equals(id, selectFieldOption.getId()))
      .findFirst();
    return optionalValue.isPresent() ? optionalValue.get().getValue() : EMPTY;
  }

  private String getIdentifier(User user, String identifierType) {
    try {
      switch (IdentifierType.fromValue(identifierType)) {
        case BARCODE:
          return user.getBarcode();
        case USER_NAME:
          return user.getUsername();
        case EXTERNAL_SYSTEM_ID:
          return user.getExternalSystemId();
        default:
          return user.getId();
      }
    } catch (IllegalArgumentException e) {
      return user.getId();
    }
  }
}
