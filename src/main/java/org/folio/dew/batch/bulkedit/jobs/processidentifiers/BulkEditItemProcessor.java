package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.Address;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.service.UserReferenceService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemProcessor implements ItemProcessor<String, UserFormat> {
  private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSX");

  private final UserClient userClient;
  private final UserReferenceService userReferenceService;

  @Override
  public UserFormat process(String id) {
    var user = userClient.getUserById(id);
    var patronGroup = userReferenceService.getUserGroupById(user.getPatronGroup());
    return UserFormat.builder()
      .username(user.getUsername())
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
      .dateOfBirth(dateFormat.format(user.getPersonal().getDateOfBirth()))
      .addresses(addressesToString(user.getPersonal().getAddresses()))
      .preferredContactTypeId(user.getPersonal().getPreferredContactTypeId())
      .enrollmentDate(dateFormat.format(user.getEnrollmentDate()))
      .expirationDate(dateFormat.format(user.getExpirationDate()))
      .createdDate(dateFormat.format(user.getCreatedDate()))
      .updatedDate(dateFormat.format(user.getUpdatedDate()))
      .tags(nonNull(user.getTags()) ? String.join(";", user.getTags().getTagList()) : EMPTY)
      .build();
  }

  private String fetchDepartments(User user) {
    if (nonNull(user.getDepartments())) {
      return user.getDepartments().stream()
        .map(id -> userReferenceService.getDepartmentById(id).getName())
        .collect(Collectors.joining(";"));
    }
    return EMPTY;
  }

  private String fetchProxyFor(User user) {
    if (nonNull(user.getProxyFor())) {
      return user.getProxyFor().stream()
        .map(id -> userReferenceService.getProxyForById(id).getProxyUserId())
        .map(userId -> userClient.getUserById(userId).getUsername())
        .collect(Collectors.joining(";"));
    }
    return EMPTY;
  }

  private String addressesToString(List<Address> addresses) {
    if (nonNull(addresses)) {
      addresses.stream()
        .map(this::addressToString)
        .collect(Collectors.joining("|"));
    }
    return EMPTY;
  }

  private String addressToString(Address address) {
    List<String> addressData = new ArrayList<>();
    addressData.add(address.getId());
    addressData.add(ofNullable(address.getCountryId()).orElse(EMPTY));
    addressData.add(ofNullable(address.getAddressLine1()).orElse(EMPTY));
    addressData.add(ofNullable(address.getAddressLine2()).orElse(EMPTY));
    addressData.add(ofNullable(address.getCity()).orElse(EMPTY));
    addressData.add(ofNullable(address.getRegion()).orElse(EMPTY));
    addressData.add(ofNullable(address.getPostalCode()).orElse(EMPTY));
    if (nonNull(address.getAddressTypeId())) {
      addressData.add(userReferenceService.getAddressTypeById(address.getAddressTypeId()).getDesc());
    }
    addressData.add(nonNull(address.getPrimaryAddress()) ? address.getPrimaryAddress().toString() : EMPTY);
    return String.join(";", addressData);
  }
}
