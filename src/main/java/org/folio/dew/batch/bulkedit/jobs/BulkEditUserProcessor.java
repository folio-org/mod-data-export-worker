package org.folio.dew.batch.bulkedit.jobs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.Address;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.service.UserReferenceService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.DATE_TIME_PATTERN;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;
import static org.folio.dew.utils.Constants.KEY_VALUE_DELIMITER;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditUserProcessor implements ItemProcessor<User, UserFormat> {

  private final UserClient userClient;
  private final UserReferenceService userReferenceService;

  private DateFormat dateFormat;

  private Set<ItemIdentifier> identifiersToCheckDuplication = new HashSet<>();

  @PostConstruct
  public void postConstruct() {
    dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @Override
  public UserFormat process(User user) {
    var patronGroup = userReferenceService.getUserGroupById(user.getPatronGroup());
    return UserFormat.builder()
      .userName(user.getUsername())
      .id(user.getId())
      .externalSystemId(user.getExternalSystemId())
      .barcode(user.getBarcode())
      .active(user.getActive().toString())
      .type(user.getType())
      .patronGroup(patronGroup.getGroup())
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
      .tags(nonNull(user.getTags()) ? String.join(ARRAY_DELIMITER, user.getTags().getTagList()) : EMPTY)
      .customFields(nonNull(user.getCustomFields()) ? customFieldsToString(user.getCustomFields()) : EMPTY)
      .build();
  }

  private String fetchDepartments(User user) {
    if (nonNull(user.getDepartments())) {
      return user.getDepartments().stream()
        .map(id -> userReferenceService.getDepartmentById(id).getName())
        .collect(Collectors.joining(ARRAY_DELIMITER));
    }
    return EMPTY;
  }

  private String fetchProxyFor(User user) {
    if (nonNull(user.getProxyFor())) {
      return user.getProxyFor().stream()
        .map(id -> userReferenceService.getProxyForById(id).getProxyUserId())
        .map(userId -> userClient.getUserById(userId).getUsername())
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
    return String.join(ARRAY_DELIMITER, addressData);
  }

  private String customFieldsToString(Map<String, Object> map) {
    return map.entrySet().stream()
      .map(e -> e.getKey() + KEY_VALUE_DELIMITER + e.getValue().toString())
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String dateToString(Date date) {
    return nonNull(date) ? dateFormat.format(date) : EMPTY;
  }
}
