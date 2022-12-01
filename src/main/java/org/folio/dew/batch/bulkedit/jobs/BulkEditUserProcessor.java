package org.folio.dew.batch.bulkedit.jobs;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
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

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.Address;
import org.folio.dew.domain.dto.CustomField;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.SpecialCharacterEscaper;
import org.folio.dew.service.UserReferenceService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditUserProcessor implements ItemProcessor<User, UserFormat> {
  private final UserClient userClient;
  private final UserReferenceService userReferenceService;
  private final SpecialCharacterEscaper escaper;

  @Override
  public UserFormat process(User user) {
    return UserFormat.builder()
      .username(user.getUsername())
      .id(user.getId())
      .externalSystemId(user.getExternalSystemId())
      .barcode(user.getBarcode())
      .active(user.getActive().toString())
      .type(user.getType())
      .patronGroup(StringUtils.isEmpty(user.getPatronGroup()) ? EMPTY : userReferenceService.getUserGroupById(user.getPatronGroup()).getGroup())
      .departments(fetchDepartments(user))
      .proxyFor(fetchProxyFor(user))
      .lastName(user.getPersonal().getLastName())
      .firstName(user.getPersonal().getFirstName())
      .middleName(user.getPersonal().getMiddleName())
      .preferredFirstName(user.getPersonal().getPreferredFirstName())
      .email(user.getPersonal().getEmail())
      .phone(user.getPersonal().getPhone())
      .mobilePhone(user.getPersonal().getMobilePhone())
      .dateOfBirth(dateToString(user.getPersonal().getDateOfBirth()))
      .addresses(addressesToString(user.getPersonal().getAddresses()))
      .preferredContactTypeId(user.getPersonal().getPreferredContactTypeId())
      .enrollmentDate(dateToString(user.getEnrollmentDate()))
      .expirationDate(dateToString(user.getExpirationDate()))
      .createdDate(dateToString(user.getCreatedDate()))
      .updatedDate(dateToString(user.getUpdatedDate()))
      .tags(nonNull(user.getTags()) ? String.join(ARRAY_DELIMITER, escaper.escape(user.getTags().getTagList())) : EMPTY)
      .customFields(nonNull(user.getCustomFields()) ? customFieldsToString(user.getCustomFields()) : EMPTY)
      .build();
  }

  private String fetchDepartments(User user) {
    if (nonNull(user.getDepartments())) {
      return user.getDepartments().stream()
        .map(id -> userReferenceService.getDepartmentById(id.toString()).getName())
        .map(escaper::escape)
        .collect(Collectors.joining(ARRAY_DELIMITER));
    }
    return EMPTY;
  }

  private String fetchProxyFor(User user) {
    if (nonNull(user.getProxyFor())) {
      return user.getProxyFor().stream()
        .map(id -> userReferenceService.getProxyForById(id).getProxyUserId())
        .map(userId -> userClient.getUserById(userId).getUsername())
        .map(escaper::escape)
        .collect(Collectors.joining(ARRAY_DELIMITER));
    }
    return EMPTY;
  }

  private String addressesToString(List<Address> addresses) {
    if (nonNull(addresses)) {
      return addresses.stream()
        .map(this::addressToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
    }
    return EMPTY;
  }

  private String addressToString(Address address) {
    List<String> addressData = new ArrayList<>();
    addressData.add(ofNullable(address.getId()).orElse(EMPTY));
    addressData.add(ofNullable(address.getCountryId()).orElse(EMPTY));
    addressData.add(ofNullable(address.getAddressLine1()).orElse(EMPTY));
    addressData.add(ofNullable(address.getAddressLine2()).orElse(EMPTY));
    addressData.add(ofNullable(address.getCity()).orElse(EMPTY));
    addressData.add(ofNullable(address.getRegion()).orElse(EMPTY));
    addressData.add(ofNullable(address.getPostalCode()).orElse(EMPTY));
    addressData.add(nonNull(address.getPrimaryAddress()) ? address.getPrimaryAddress().toString() : EMPTY);
    if (nonNull(address.getAddressTypeId())) {
      addressData.add(userReferenceService.getAddressTypeById(address.getAddressTypeId()).getDesc());
    }
    return String.join(ARRAY_DELIMITER, escaper.escape(addressData));
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
}
