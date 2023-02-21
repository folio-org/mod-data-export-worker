package org.folio.dew.batch.bulkedit.jobs;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
import static org.folio.dew.utils.BulkEditProcessorHelper.ofEmptyString;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;
import static org.folio.dew.utils.Constants.KEY_VALUE_DELIMITER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.Address;
import org.folio.dew.domain.dto.CustomField;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.Personal;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.SpecialCharacterEscaper;
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
  private final SpecialCharacterEscaper escaper;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;

  @Override
  public UserFormat process(User user) {
    var errorServiceArgs = new ErrorServiceArgs(jobId, getIdentifier(user, identifierType), FilenameUtils.getName(fileName));
    var personal = user.getPersonal();
    return UserFormat.builder()
      .username(user.getUsername())
      .id(user.getId())
      .externalSystemId(user.getExternalSystemId())
      .barcode(user.getBarcode())
      .active((isNull(user.getActive()) ? Boolean.FALSE : user.getActive()).toString())
      .type(user.getType())
      .patronGroup(userReferenceService.getPatronGroupNameById(user.getPatronGroup(), errorServiceArgs))
      .departments(fetchDepartments(user, errorServiceArgs))
      .lastName(nonNull(personal) ? personal.getLastName() : EMPTY)
      .firstName(nonNull(personal) ? personal.getFirstName() : EMPTY)
      .middleName(nonNull(personal) ? personal.getMiddleName() : EMPTY)
      .preferredFirstName(nonNull(personal)? personal.getPreferredFirstName() : EMPTY)
      .email(nonNull(personal) ? personal.getEmail() : EMPTY)
      .phone(nonNull(personal) ? personal.getPhone() : EMPTY)
      .mobilePhone(nonNull(personal) ? personal.getMobilePhone() : EMPTY)
      .dateOfBirth(nonNull(personal) ? dateToString(personal.getDateOfBirth()) : EMPTY)
      .addresses(nonNull(personal)? addressesToString(personal.getAddresses(), errorServiceArgs) : EMPTY)
      .preferredContactTypeId(nonNull(personal) ? getPreferredContactTypeId(personal) : EMPTY)
      .enrollmentDate(dateToString(user.getEnrollmentDate()))
      .expirationDate(dateToString(user.getExpirationDate()))
      .createdDate(dateToString(user.getCreatedDate()))
      .updatedDate(dateToString(user.getUpdatedDate()))
      .tags(nonNull(user.getTags()) ? String.join(ARRAY_DELIMITER, escaper.escape(user.getTags().getTagList())) : EMPTY)
      .customFields(nonNull(user.getCustomFields()) ? customFieldsToString(user.getCustomFields()) : EMPTY)
      .build().withOriginal(user);
  }

  private String getPreferredContactTypeId(Personal personal) {
    return isNull(personal.getPreferredContactTypeId()) ? EMPTY : personal.getPreferredContactTypeId();
  }

  private String fetchDepartments(User user, ErrorServiceArgs args) {
    if (nonNull(user.getDepartments())) {
      return user.getDepartments().stream()
        .map(id -> userReferenceService.getDepartmentNameById(id.toString(), args))
        .filter(StringUtils::isNotEmpty)
        .map(escaper::escape)
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
    List<String> data = new ArrayList<>();
    ofEmptyString(address.getId()).ifPresent(data::add);
    ofEmptyString(address.getCountryId()).ifPresent(data::add);
    ofEmptyString(address.getAddressLine1()).ifPresent(data::add);
    ofEmptyString(address.getAddressLine2()).ifPresent(data::add);
    ofEmptyString(address.getCity()).ifPresent(data::add);
    ofEmptyString(address.getRegion()).ifPresent(data::add);
    ofEmptyString(address.getPostalCode()).ifPresent(data::add);
    ofNullable(address.getPrimaryAddress()).ifPresent(primary -> data.add(primary.toString()));
    ofEmptyString(userReferenceService.getAddressTypeDescById(address.getAddressTypeId(), args)).ifPresent(data::add);
    return String.join(ARRAY_DELIMITER, escaper.escape(data));
  }

  private String customFieldsToString(Map<String, Object> map) {
    return map.entrySet().stream()
      .map(this::customFieldToString)
      .filter(StringUtils::isNotEmpty)
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String customFieldToString(Map.Entry<String, Object> entry) {
    var customField = userReferenceService.getCustomFieldByRefId(entry.getKey());
    switch (customField.getType()) {
    case TEXTBOX_LONG:
    case TEXTBOX_SHORT:
    case SINGLE_CHECKBOX:
      if (entry.getValue() instanceof String) {
        return escaper.escape(customField.getName()) + KEY_VALUE_DELIMITER + escaper.escape((String) entry.getValue());
      } else {
        return escaper.escape(customField.getName()) + KEY_VALUE_DELIMITER + entry.getValue();
      }
    case SINGLE_SELECT_DROPDOWN:
    case RADIO_BUTTON:
      return escaper.escape(customField.getName()) + KEY_VALUE_DELIMITER + escaper.escape(extractValueById(customField, entry.getValue().toString()));
    case MULTI_SELECT_DROPDOWN:
      var values = (ArrayList) entry.getValue();
      return escaper.escape(customField.getName()) + KEY_VALUE_DELIMITER + values.stream()
        .map(v -> escaper.escape(extractValueById(customField, v.toString())))
        .filter(ObjectUtils::isNotEmpty)
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
