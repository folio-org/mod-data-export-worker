package org.folio.dew.service;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateFromString;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.dew.utils.Constants.KEY_VALUE_DELIMITER;
import static org.folio.dew.utils.Constants.LINE_BREAK;
import static org.folio.dew.utils.Constants.LINE_BREAK_REPLACEMENT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.dew.domain.dto.Address;
import org.folio.dew.domain.dto.CirculationNote;
import org.folio.dew.domain.dto.ContributorName;
import org.folio.dew.domain.dto.CustomField;

import org.folio.dew.domain.dto.InventoryItemStatus;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.domain.dto.ItemLocation;
import org.folio.dew.domain.dto.ItemNote;
import org.folio.dew.domain.dto.LastCheckIn;
import org.folio.dew.domain.dto.LoanType;
import org.folio.dew.domain.dto.MaterialType;
import org.folio.dew.domain.dto.Personal;
import org.folio.dew.domain.dto.Source;
import org.folio.dew.domain.dto.Tags;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Log4j2
public class BulkEditParseService {

  private final UserReferenceService userReferenceService;
  private final ItemReferenceService itemReferenceService;

  private final ElectronicAccessService electronicAccessService;

  private static final int ADDRESS_ID = 0;
  private static final int ADDRESS_COUNTRY_ID = 1;
  private static final int ADDRESS_LINE_1 = 2;
  private static final int ADDRESS_LINE_2 = 3;
  private static final int ADDRESS_CITY = 4;
  private static final int ADDRESS_REGION = 5;
  private static final int ADDRESS_POSTAL_CODE = 6;
  private static final int ADDRESS_PRIMARY_ADDRESS = 7;
  private static final int ADDRESS_TYPE = 8;
  private static final int NUMBER_OF_ITEM_NOTE_COMPONENTS = 3;
  private static final int NOTE_TYPE_NAME_INDEX = 0;
  private static final int NOTE_INDEX = 1;
  private static final int STAFF_ONLY_OFFSET = 1;

  private static final int NUMBER_OF_CIRCULATION_NOTE_COMPONENTS = 8;
  private static final int CIRC_NOTE_ID_INDEX = 0;
  private static final int CIRC_NOTE_TYPE_INDEX = 1;
  private static final int CIRC_NOTE_NOTE_INDEX = 2;
  private static final int CIRC_NOTE_STAFF_ONLY_OFFSET = 5;
  private static final int CIRC_NOTE_SOURCE_ID_OFFSET = 4;
  private static final int CIRC_NOTE_LAST_NAME_OFFSET = 3;
  private static final int CIRC_NOTE_FIRST_NAME_OFFSET = 2;
  private static final int CIRC_NOTE_DATE_OFFSET = 1;

  private static final int NUMBER_OF_STATUS_COMPONENTS = 2;
  private static final int STATUS_NAME_INDEX = 0;
  private static final int STATUS_DATE_INDEX = 1;
    private static final int NUMBER_OF_LAST_CHECK_IN_COMPONENTS = 3;
  private static final int LAST_CHECK_IN_SERVICE_POINT_NAME_INDEX = 0;
  private static final int LAST_CHECK_IN_USERNAME_INDEX = 1;
  private static final int LAST_CHECK_IN_DATE_TIME_INDEX = 2;

  private static final String START_ARRAY = "[";
  private static final String END_ARRAY = "]";

  public User mapUserFormatToUser(UserFormat userFormat) {
    User user = new User();
    populateUserFields(user, userFormat);
    return user;
  }

  private void populateUserFields(User user, UserFormat userFormat) {
    user.setId(userFormat.getId());
    user.setUsername(isEmpty(userFormat.getUsername()) ? null : userFormat.getUsername());
    user.setExternalSystemId(isBlank(userFormat.getExternalSystemId()) ? null : userFormat.getExternalSystemId());
    user.setBarcode(isBlank(userFormat.getBarcode()) ? null : userFormat.getBarcode());
    user.setActive(getIsActive(userFormat));
    user.setType(userFormat.getType());
    user.setPatronGroup(userReferenceService.getPatronGroupIdByName(userFormat.getPatronGroup()));
    user.setDepartments(getUserDepartments(userFormat));
    user.setProxyFor(isEmpty(userFormat.getProxyFor()) ? Collections.emptyList() : Arrays.asList(userFormat.getProxyFor().split(ARRAY_DELIMITER)));
    user.setPersonal(getUserPersonalInfo(userFormat));
    user.setEnrollmentDate(dateFromString(userFormat.getEnrollmentDate()));
    user.setExpirationDate(dateFromString(userFormat.getExpirationDate()));
    user.setCreatedDate(dateFromString(userFormat.getCreatedDate()));
    user.setUpdatedDate(dateFromString(userFormat.getUpdatedDate()));
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

  private List<UUID> getUserDepartments(UserFormat userFormat) {
    String[] departmentNames = userFormat.getDepartments().split(ARRAY_DELIMITER);
    if (departmentNames.length > 0) {
      return Arrays.stream(departmentNames).parallel()
        .filter(StringUtils::isNotEmpty)
        .map(userReferenceService::getDepartmentIdByName)
        .map(UUID::fromString)
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
    personal.setDateOfBirth(dateFromString(userFormat.getDateOfBirth()));
    personal.setAddresses(getUserAddresses(userFormat));
    if (isEmpty(userFormat.getPreferredContactTypeId())) {
      throw new BulkEditException("Missing required data: Preferred contact - record cannot be updated");
    }
    personal.setPreferredContactTypeId(userFormat.getPreferredContactTypeId());
    return personal;
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
    address.setAddressTypeId(userReferenceService.getAddressTypeIdByDesc(addressFields.get(ADDRESS_TYPE)));
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
      return Arrays.stream(userFormat.getCustomFields().split(ITEM_DELIMITER_PATTERN))
        .map(this::restoreCustomFieldValue)
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }
    return Collections.emptyMap();
  }

  private Pair<String, Object> restoreCustomFieldValue(String s) {
    var valuePair = stringToPair(s);
    var fieldName = valuePair.getKey();
    var fieldValue = valuePair.getValue();
    var customField = userReferenceService.getCustomFieldByName(fieldName);
    switch (customField.getType()) {
    case SINGLE_CHECKBOX:
      return Pair.of(customField.getRefId(), Boolean.parseBoolean(fieldValue));
    case TEXTBOX_LONG:
    case TEXTBOX_SHORT:
      return Pair.of(customField.getRefId(), fieldValue.replace(LINE_BREAK_REPLACEMENT, LINE_BREAK));
    case SINGLE_SELECT_DROPDOWN:
    case RADIO_BUTTON:
      return Pair.of(customField.getRefId(), restoreValueId(customField, fieldValue));
    case MULTI_SELECT_DROPDOWN:
      return Pair.of(customField.getRefId(), restoreValueIds(customField, fieldValue));
    default:
      throw new BulkEditException("Invalid custom field: " + s);
    }
  }

  private Pair<String, String> stringToPair(String value) {
    var tokens = value.split(KEY_VALUE_DELIMITER, -1);
    if (tokens.length == 2) {
      return Pair.of(tokens[0], tokens[1]);
    } else {
      var msg = "Invalid key/value pair: " + value;
      log.error(msg);
      throw new BulkEditException(msg);
    }
  }

  private List<String> restoreValueIds(CustomField customField, String values) {
    return isEmpty(values) ?
      Collections.emptyList() :
      Arrays.stream(values.split(ARRAY_DELIMITER))
        .map(token -> restoreValueId(customField, token))
        .collect(Collectors.toList());
  }

  private String restoreValueId(CustomField customField, String value) {
    var optionalValue = customField.getSelectField().getOptions().getValues().stream()
      .filter(selectFieldOption -> Objects.equals(value, selectFieldOption.getValue()))
      .findFirst();
    if (optionalValue.isPresent()) {
      return optionalValue.get().getId();
    } else {
      var msg = "Invalid custom field value: " + value;
      log.error(msg);
      throw new BulkEditException(msg);
    }
  }

  public Item mapItemFormatToItem(ItemFormat itemFormat) {
    return new Item()
      .id(itemFormat.getId())
      .version(isEmpty(itemFormat.getVersion()) ? null : Integer.parseInt(itemFormat.getVersion()))
      .hrid(itemFormat.getHrid())
      .holdingsRecordId(itemFormat.getHoldingsRecordId())
      .formerIds(restoreListValue(itemFormat.getFormerIds()))
      .discoverySuppress(isEmpty(itemFormat.getDiscoverySuppress()) ? null : Boolean.valueOf(itemFormat.getDiscoverySuppress()))
      .title(itemFormat.getTitle())
      .contributorNames(restoreContributorNames(itemFormat.getContributorNames()))
      .callNumber(restoreStringValue(itemFormat.getCallNumber()))
      .barcode(restoreStringValue(itemFormat.getBarcode()))
      .effectiveShelvingOrder(restoreStringValue(itemFormat.getEffectiveShelvingOrder()))
      .accessionNumber(restoreStringValue(itemFormat.getAccessionNumber()))
      .itemLevelCallNumber(restoreStringValue(itemFormat.getItemLevelCallNumber()))
      .itemLevelCallNumberPrefix(restoreStringValue(itemFormat.getItemLevelCallNumberPrefix()))
      .itemLevelCallNumberSuffix(restoreStringValue(itemFormat.getItemLevelCallNumberSuffix()))
      .itemLevelCallNumberTypeId(restoreItemLevelCallNumberTypeId(itemFormat.getItemLevelCallNumberType()))
      .volume(restoreStringValue(itemFormat.getVolume()))
      .enumeration(restoreStringValue(itemFormat.getEnumeration()))
      .chronology(restoreStringValue(itemFormat.getChronology()))
      .yearCaption(restoreListValue(itemFormat.getYearCaption()))
      .itemIdentifier(restoreStringValue(itemFormat.getItemIdentifier()))
      .copyNumber(restoreStringValue(itemFormat.getCopyNumber()))
      .numberOfPieces(restoreStringValue(itemFormat.getNumberOfPieces()))
      .descriptionOfPieces(restoreStringValue(itemFormat.getDescriptionOfPieces()))
      .numberOfMissingPieces(restoreStringValue(itemFormat.getNumberOfMissingPieces()))
      .missingPieces(restoreStringValue(itemFormat.getMissingPieces()))
      .missingPiecesDate(restoreStringValue(itemFormat.getMissingPiecesDate()))
      .itemDamagedStatusId(restoreItemDamagedStatusId(itemFormat.getItemDamagedStatus()))
      .itemDamagedStatusDate(restoreStringValue(itemFormat.getItemDamagedStatusDate()))
      .administrativeNotes(restoreListValue(itemFormat.getAdministrativeNotes()))
      .notes(restoreItemNotes(itemFormat.getNotes()))
      .circulationNotes(restoreCirculationNotes(itemFormat.getCirculationNotes()))
      .status(restoreStatus(itemFormat.getStatus()))
      .materialType(restoreMaterialType(itemFormat.getMaterialType()))
      .permanentLoanType(restoreLoanType(itemFormat.getPermanentLoanType()))
      .temporaryLoanType(restoreLoanType(itemFormat.getTemporaryLoanType()))
      .permanentLocation(restoreLocation(itemFormat.getPermanentLocation()))
      .temporaryLocation(restoreLocation(itemFormat.getTemporaryLocation()))
      .effectiveLocation(restoreLocation(itemFormat.getEffectiveLocation()))
      .electronicAccess(electronicAccessService.restoreElectronicAccess(itemFormat.getElectronicAccess()))
      .inTransitDestinationServicePointId(restoreServicePointId(itemFormat.getInTransitDestinationServicePoint()))
      .statisticalCodeIds(restoreStatisticalCodeIds(itemFormat.getStatisticalCodes()))
      .purchaseOrderLineIdentifier(restoreStringValue(itemFormat.getPurchaseOrderLineIdentifier()))
      .tags(isEmpty(itemFormat.getTags()) ? new Tags().tagList(Collections.emptyList()) : new Tags().tagList(restoreListValue(itemFormat.getTags())))
      .lastCheckIn(restoreLastCheckIn(itemFormat.getLastCheckIn()));
  }

  private String restoreStringValue(String s) {
    return isEmpty(s) || "null".equalsIgnoreCase(s) ? null : s;
  }

  private List<String> restoreListValue(String s) {
    return isEmpty(s) ?
      Collections.emptyList() :
      Arrays.asList(s.split(ARRAY_DELIMITER));
  }

  private List<ContributorName> restoreContributorNames(String s) {
    return isEmpty(s) ?
      Collections.emptyList() :
      Arrays.stream(s.split(ARRAY_DELIMITER))
        .map(token -> new ContributorName().name(token))
        .collect(Collectors.toList());
  }

  private String restoreItemLevelCallNumberTypeId(String name) {
    return itemReferenceService.getCallNumberTypeIdByName(name);
  }

  private String restoreItemDamagedStatusId(String name) {
    return itemReferenceService.getDamagedStatusIdByName(name);
  }

  private List<ItemNote> restoreItemNotes(String s) {
    return isEmpty(s) ? Collections.emptyList() :
      Arrays.stream(s.split(ITEM_DELIMITER_PATTERN))
        .map(this::restoreItemNote)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private ItemNote restoreItemNote(String s) {
    if (isNotEmpty(s)) {
      var tokens = s.split(ARRAY_DELIMITER, -1);
      if (tokens.length < NUMBER_OF_ITEM_NOTE_COMPONENTS) {
        throw new BulkEditException(String.format("Illegal number of item note elements: %d, expected: %d", tokens.length, NUMBER_OF_ITEM_NOTE_COMPONENTS));
      }
      return new ItemNote()
        .itemNoteTypeId(itemReferenceService.getNoteTypeIdByName(tokens[NOTE_TYPE_NAME_INDEX]))
        .note(Arrays.stream(tokens, NOTE_INDEX, tokens.length - STAFF_ONLY_OFFSET)
          .collect(Collectors.joining(";")))
        .staffOnly(Boolean.valueOf(tokens[tokens.length - STAFF_ONLY_OFFSET]));

    }
    return null;
  }

  private List<CirculationNote> restoreCirculationNotes(String s) {
    return isEmpty(s) ? Collections.emptyList() :
      Arrays.stream(s.split(ITEM_DELIMITER_PATTERN))
        .map(this::restoreCirculationNote)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private CirculationNote restoreCirculationNote(String s) {
    if (isNotEmpty(s)) {
      var tokens = s.split(ARRAY_DELIMITER, -1);
      if (tokens.length < NUMBER_OF_CIRCULATION_NOTE_COMPONENTS) {
        throw new BulkEditException(String.format("Illegal number of circulation note elements: %d, expected: %d", tokens.length, NUMBER_OF_CIRCULATION_NOTE_COMPONENTS));
      }
      return new CirculationNote()
        .id(tokens[CIRC_NOTE_ID_INDEX])
        .noteType(CirculationNote.NoteTypeEnum.fromValue(tokens[CIRC_NOTE_TYPE_INDEX]))
        .note(Arrays.stream(tokens, CIRC_NOTE_NOTE_INDEX, tokens.length - CIRC_NOTE_STAFF_ONLY_OFFSET)
          .collect(Collectors.joining(";")))
        .staffOnly(Boolean.valueOf(tokens[tokens.length - CIRC_NOTE_STAFF_ONLY_OFFSET]))
        .source(new Source()
          .id(tokens[tokens.length - CIRC_NOTE_SOURCE_ID_OFFSET])
          .personal(new Personal()
            .lastName(tokens[tokens.length - CIRC_NOTE_LAST_NAME_OFFSET])
            .firstName(tokens[tokens.length - CIRC_NOTE_FIRST_NAME_OFFSET])))
        .date(dateFromString(tokens[tokens.length - CIRC_NOTE_DATE_OFFSET]));
    }
    return null;
  }

  private InventoryItemStatus restoreStatus(String s) {
    if (isNotEmpty(s)) {
      var tokens = s.split(ARRAY_DELIMITER, -1);
      if (NUMBER_OF_STATUS_COMPONENTS == tokens.length) {
        return new InventoryItemStatus()
          .name(InventoryItemStatus.NameEnum.fromValue(tokens[STATUS_NAME_INDEX]))
          .date(dateFromString(tokens[STATUS_DATE_INDEX]));
      }
      throw new BulkEditException(String.format("Illegal number of item status elements: %d, expected: %d", tokens.length, NUMBER_OF_STATUS_COMPONENTS));
    }
    return null;
  }

  private MaterialType restoreMaterialType(String s) {
    return isEmpty(s) ? null : itemReferenceService.getMaterialTypeByName(s);
  }

  private LoanType restoreLoanType(String s) {
    return isEmpty(s) ? null : itemReferenceService.getLoanTypeByName(s);
  }

  private ItemLocation restoreLocation(String s) {
    return isEmpty(s) ? null : itemReferenceService.getLocationByName(s);
  }

  private String restoreServicePointId(String s) {
    return itemReferenceService.getServicePointIdByName(s);
  }

  private List<String> restoreStatisticalCodeIds(String s) {
    return isEmpty(s) ? Collections.emptyList() :
      Arrays.stream(s.split(ARRAY_DELIMITER))
        .map(itemReferenceService::getStatisticalCodeIdByCode)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private LastCheckIn restoreLastCheckIn(String s) {
    if (isNotEmpty(s)) {
      var tokens = s.split(ARRAY_DELIMITER, -1);
      if (NUMBER_OF_LAST_CHECK_IN_COMPONENTS == tokens.length) {
        return new LastCheckIn()
          .servicePointId(itemReferenceService.getServicePointIdByName(tokens[LAST_CHECK_IN_SERVICE_POINT_NAME_INDEX]))
          .staffMemberId(itemReferenceService.getUserIdByUserName(tokens[LAST_CHECK_IN_USERNAME_INDEX]))
          .dateTime(restoreStringValue(tokens[LAST_CHECK_IN_DATE_TIME_INDEX]));
      }
      throw new BulkEditException(String.format("Illegal number of last check in elements: %d, expected: %d", tokens.length, NUMBER_OF_LAST_CHECK_IN_COMPONENTS));
    }
    return null;
  }
}
