package org.folio.dew.batch.bulkedit.jobs;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.BulkEditProcessorHelper.booleanToStringNullSafe;
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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.Address;
import org.folio.dew.domain.dto.CustomField;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.Personal;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
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
    var personal = isNull(user.getPersonal()) ? new Personal() : user.getPersonal();
    return UserFormat.builder()
      .username(user.getUsername())
      .id(user.getId())
      .externalSystemId(user.getExternalSystemId())
      .barcode(user.getBarcode())
      .active(booleanToStringNullSafe(user.getActive()))
      .type(user.getType())
      .patronGroup(userReferenceService.getPatronGroupNameById(user.getPatronGroup(), errorServiceArgs))
      .departments(fetchDepartments(user, errorServiceArgs))
      .proxyFor(proxyForToString(user.getProxyFor()))
      .lastName(ofNullable(personal.getLastName()).orElse(EMPTY))
      .firstName(ofNullable(personal.getFirstName()).orElse(EMPTY))
      .middleName(ofNullable(personal.getMiddleName()).orElse(EMPTY))
      .preferredFirstName(ofNullable(personal.getPreferredFirstName()).orElse(EMPTY))
      .email(ofNullable(personal.getEmail()).orElse(EMPTY))
      .phone(ofNullable(personal.getPhone()).orElse(EMPTY))
      .mobilePhone(ofNullable(personal.getMobilePhone()).orElse(EMPTY))
      .dateOfBirth(dateToString(personal.getDateOfBirth()))
      .addresses(addressesToString(personal.getAddresses(), errorServiceArgs))
      .preferredContactTypeId(ofNullable(personal.getPreferredContactTypeId()).orElse(EMPTY))
      .enrollmentDate(dateToString(user.getEnrollmentDate()))
      .expirationDate(dateToString(user.getExpirationDate()))
      .createdDate(dateToString(user.getCreatedDate()))
      .updatedDate(dateToString(user.getUpdatedDate()))
      .tags(nonNull(user.getTags()) ? String.join(ARRAY_DELIMITER, escaper.escape(user.getTags().getTagList())) : EMPTY)
      .customFields(nonNull(user.getCustomFields()) ? customFieldsToString(user.getCustomFields()) : EMPTY)
      .build().withOriginal(user);
  }

  private String fetchDepartments(User user, ErrorServiceArgs args) {
    return isEmpty(user.getDepartments()) ?
      EMPTY :
      user.getDepartments().stream()
        .filter(Objects::nonNull)
        .map(id -> userReferenceService.getDepartmentNameById(id.toString(), args))
        .filter(StringUtils::isNotEmpty)
        .map(escaper::escape)
        .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private String proxyForToString(List<String> values) {
    return ObjectUtils.isEmpty(values) ?
      EMPTY :
      values.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private String addressesToString(List<Address> addresses, ErrorServiceArgs args) {
    return isEmpty(addresses) ?
      EMPTY :
      addresses.stream()
        .filter(Objects::nonNull)
        .map(address -> addressToString(address, args))
        .collect(Collectors.joining(ITEM_DELIMITER));
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
    addressData.add(booleanToStringNullSafe(address.getPrimaryAddress()));
    addressData.add(userReferenceService.getAddressTypeDescById(address.getAddressTypeId(), args));
    return String.join(ARRAY_DELIMITER, escaper.escape(addressData));
  }

  private String customFieldsToString(Map<String, Object> map) {
    return map.entrySet().stream()
      .map(this::customFieldToString)
      .filter(StringUtils::isNotEmpty)
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String customFieldToString(Map.Entry<String, Object> entry) {
    var customField = userReferenceService.getCustomFieldByRefId(entry.getKey());
    return switch (customField.getType()) {
    case TEXTBOX_LONG, TEXTBOX_SHORT, SINGLE_CHECKBOX ->
      escaper.escape(customField.getName()) + KEY_VALUE_DELIMITER + (isNull(entry.getValue()) ? EMPTY : escaper.escape(entry.getValue().toString()));
    case SINGLE_SELECT_DROPDOWN, RADIO_BUTTON ->
      escaper.escape(customField.getName()) + KEY_VALUE_DELIMITER + (isNull(entry.getValue()) ? EMPTY : escaper.escape(extractValueById(customField, entry.getValue().toString())));
    case MULTI_SELECT_DROPDOWN ->
      escaper.escape(customField.getName()) + KEY_VALUE_DELIMITER +
        (entry.getValue() instanceof ArrayList<?> list ?
          list.stream()
            .filter(Objects::nonNull)
            .map(v -> escaper.escape(extractValueById(customField, v.toString())))
            .filter(ObjectUtils::isNotEmpty)
            .collect(Collectors.joining(ARRAY_DELIMITER)) :
          EMPTY);
    };
  }

  private String extractValueById(CustomField customField, String id) {
    var optionalValue = customField.getSelectField().getOptions().getValues().stream()
      .filter(selectFieldOption -> Objects.equals(id, selectFieldOption.getId()))
      .findFirst();
    return optionalValue.isPresent() ? optionalValue.get().getValue() : EMPTY;
  }

  private String getIdentifier(User user, String identifierType) {
    try {
      return switch (IdentifierType.fromValue(identifierType)) {
        case BARCODE -> user.getBarcode();
        case USER_NAME -> user.getUsername();
        case EXTERNAL_SYSTEM_ID -> user.getExternalSystemId();
        default -> user.getId();
      };
    } catch (IllegalArgumentException e) {
      return user.getId();
    }
  }
}
