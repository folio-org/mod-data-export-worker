package org.folio.dew.batch.bulkedit.jobs;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.dew.utils.BulkEditProcessorHelper.booleanToStringNullSafe;
import static org.folio.dew.utils.BulkEditProcessorHelper.ofEmptyString;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.CALL_NUMBER;
import static org.folio.dew.utils.Constants.CALL_NUMBER_PREFIX;
import static org.folio.dew.utils.Constants.CALL_NUMBER_SUFFIX;
import static org.folio.dew.utils.Constants.HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_SPACED;
import static org.folio.dew.utils.Constants.PERMANENT_LOCATION_ID;
import static org.folio.dew.utils.Constants.STAFF_ONLY;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.batch.acquisitions.edifact.services.HoldingService;
import org.folio.dew.domain.dto.CirculationNote;
import org.folio.dew.domain.dto.ContributorName;
import org.folio.dew.domain.dto.EffectiveCallNumberComponents;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.ExtendedItem;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.domain.dto.Title;
import org.folio.dew.service.ElectronicAccessService;
import org.folio.dew.service.HoldingsReferenceService;
import org.folio.dew.service.ItemReferenceService;
import org.folio.dew.service.SpecialCharacterEscaper;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemProcessor implements ItemProcessor<ExtendedItem, ItemFormat> {
  private static final String IS_ACTIVE = "isActive";
  private static final String NAME = "name";

  private final ItemReferenceService itemReferenceService;
  private final HoldingsReferenceService holdingsReferenceService;
  private final ElectronicAccessService electronicAccessService;
  private final SpecialCharacterEscaper escaper;
  private final HoldingService holdingService;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;

  @Override
  public ItemFormat process(ExtendedItem extendedItem) {
    var item = extendedItem.getEntity();
    var tenantId = extendedItem.getTenantId();
    var errorServiceArgs = new ErrorServiceArgs(jobId, getIdentifier(item, identifierType), FilenameUtils.getName(fileName));
    var itemFormat = ItemFormat.builder()
      .id(item.getId())
      .hrid(item.getHrid())
      .holdingsRecordId(item.getHoldingsRecordId())
      .formerIds(isEmpty(item.getFormerIds()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(item.getFormerIds())))
      .discoverySuppress(booleanToStringNullSafe(item.getDiscoverySuppress()))
      .title(getInstanceTitle(item, tenantId))
      .holdingsData(getHoldingsName(item.getHoldingsRecordId(), tenantId))
      .barcode(item.getBarcode())
      .effectiveShelvingOrder(item.getEffectiveShelvingOrder())
      .accessionNumber(item.getAccessionNumber())
      .itemLevelCallNumber(item.getItemLevelCallNumber())
      .itemLevelCallNumberPrefix(item.getItemLevelCallNumberPrefix())
      .itemLevelCallNumberSuffix(item.getItemLevelCallNumberSuffix())
      .itemLevelCallNumberType(itemReferenceService.getCallNumberTypeNameById(item.getItemLevelCallNumberTypeId(), errorServiceArgs, tenantId))
      .effectiveCallNumberComponents(effectiveCallNumberComponentsToString(item.getEffectiveCallNumberComponents()))
      .volume(item.getVolume())
      .enumeration(item.getEnumeration())
      .chronology(item.getChronology())
      .yearCaption(isEmpty(item.getYearCaption()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(item.getYearCaption())))
      .itemIdentifier(item.getItemIdentifier())
      .copyNumber(item.getCopyNumber())
      .numberOfPieces(item.getNumberOfPieces())
      .descriptionOfPieces(item.getDescriptionOfPieces())
      .numberOfMissingPieces(item.getNumberOfMissingPieces())
      .missingPieces(item.getMissingPieces())
      .missingPiecesDate(item.getMissingPiecesDate())
      .itemDamagedStatus(itemReferenceService.getDamagedStatusNameById(item.getItemDamagedStatusId(), errorServiceArgs, tenantId))
      .itemDamagedStatusDate(item.getItemDamagedStatusDate())
      .administrativeNotes(isEmpty(item.getAdministrativeNotes()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(item.getAdministrativeNotes())))
      .notes(fetchNotes(item, errorServiceArgs))
      .checkInNotes(fetchCirculationNotes(item, CirculationNote.NoteTypeEnum.IN))
      .checkOutNotes(fetchCirculationNotes(item, CirculationNote.NoteTypeEnum.OUT))
      .status(statusToString(item))
      .materialType(isEmpty(item.getMaterialType()) ? EMPTY : item.getMaterialType().getName())
      .isBoundWith(booleanToStringNullSafe(item.getIsBoundWith()))
      .boundWithTitles(fetchBoundWithTitles(item))
      .permanentLoanType(isEmpty(item.getPermanentLoanType()) ? EMPTY : item.getPermanentLoanType().getName())
      .temporaryLoanType(isEmpty(item.getTemporaryLoanType()) ? EMPTY : item.getTemporaryLoanType().getName())
      .permanentLocation(isEmpty(item.getPermanentLocation()) ? EMPTY : item.getPermanentLocation().getName())
      .temporaryLocation(isEmpty(item.getTemporaryLocation()) ? EMPTY : item.getTemporaryLocation().getName())
      .effectiveLocation(isEmpty(item.getEffectiveLocation()) ? EMPTY : item.getEffectiveLocation().getName())
      .statisticalCodes(fetchStatisticalCodes(item, errorServiceArgs))
      .tags(isEmpty(item.getTags()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(item.getTags().getTagList())))
      .build();
    itemFormat.setElectronicAccess(electronicAccessService.getElectronicAccessesToString(item.getElectronicAccess(), errorServiceArgs, tenantId));
    return itemFormat.withOriginal(item).withTenantId(tenantId);
  }

  private String getInstanceTitle(Item item, String tenantId) {
    var holding = holdingsReferenceService.getHoldingById(item.getHoldingsRecordId(), tenantId);
    if (nonNull(holding)) {
      return holdingsReferenceService.getInstanceTitleById(holding.getInstanceId(), tenantId);
    }
    return EMPTY;
  }

  private String statusToString(Item item) {
    var status = item.getStatus();
    if (nonNull(status)) {
      return isEmpty(status.getName()) ? EMPTY : status.getName().getValue();
    }
    return EMPTY;
  }

  private String fetchContributorNames(Item item) {
    return isEmpty(item.getContributorNames()) ?
      EMPTY :
      item.getContributorNames().stream()
        .filter(Objects::nonNull)
        .map(ContributorName::getName)
        .map(escaper::escape)
        .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private String effectiveCallNumberComponentsToString(EffectiveCallNumberComponents components) {
    if (isEmpty(components)) {
      return EMPTY;
    }
    List<String> entries = new ArrayList<>();
    ofEmptyString(components.getPrefix()).ifPresent(e -> entries.add(escaper.escape(e)));
    ofEmptyString(components.getCallNumber()).ifPresent(e -> entries.add(escaper.escape(e)));
    ofEmptyString(components.getSuffix()).ifPresent(e -> entries.add(escaper.escape(e)));
    return String.join(SPACE, entries);
  }

  private String fetchNotes(Item item, ErrorServiceArgs args) {
    return isEmpty(item.getNotes()) ?
      EMPTY :
      item.getNotes().stream()
        .filter(Objects::nonNull)
        .map(itemNote -> String.join(ARRAY_DELIMITER,
          escaper.escape(itemReferenceService.getNoteTypeNameById(itemNote.getItemNoteTypeId(), args)),
          escaper.escape(itemNote.getNote()),
          escaper.escape(booleanToStringNullSafe(itemNote.getStaffOnly()))))
        .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String fetchCirculationNotes(Item item, CirculationNote.NoteTypeEnum noteType) {
    return isEmpty(item.getCirculationNotes()) ?
      EMPTY :
      item.getCirculationNotes().stream()
        .filter(Objects::nonNull)
        .filter(circulationNote -> noteType.equals(circulationNote.getNoteType()))
        .map(this::circulationNotesToString)
        .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }

  private String circulationNotesToString(CirculationNote note) {
    return escaper.escape(note.getNote() + (Boolean.TRUE.equals(note.getStaffOnly()) ? SPACE + STAFF_ONLY : EMPTY));
  }

  private String fetchBoundWithTitles(Item item) {
    return isEmpty(item.getBoundWithTitles()) ?
      EMPTY :
      item.getBoundWithTitles().stream()
        .filter(Objects::nonNull)
        .map(this::titleToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String titleToString(Title title) {
    return String.join(ARRAY_DELIMITER,
      escaper.escape(isEmpty(title.getBriefHoldingsRecord()) ? EMPTY : title.getBriefHoldingsRecord().getHrid()),
      escaper.escape(isEmpty(title.getBriefInstance()) ? EMPTY : title.getBriefInstance().getHrid()),
      escaper.escape(isEmpty(title.getBriefInstance()) ? EMPTY : title.getBriefInstance().getTitle()));
  }

  private String fetchStatisticalCodes(Item item, ErrorServiceArgs args) {
    return isEmpty(item.getStatisticalCodeIds()) ?
      EMPTY :
      item.getStatisticalCodeIds().stream()
        .filter(Objects::nonNull)
        .map(id -> itemReferenceService.getStatisticalCodeById(id, args))
        .map(escaper::escape)
        .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private String lastCheckInToString(Item item, ErrorServiceArgs args, String tenantId) {
    var lastCheckIn = item.getLastCheckIn();
    if (isEmpty(lastCheckIn)) {
      return EMPTY;
    }
    return String.join(ARRAY_DELIMITER,
      escaper.escape(itemReferenceService.getServicePointNameById(lastCheckIn.getServicePointId(), args, tenantId)),
      escaper.escape(itemReferenceService.getUserNameById(lastCheckIn.getStaffMemberId(), args)),
      lastCheckIn.getDateTime());
  }

  private String getIdentifier(Item item, String identifierType) {
    try {
      return switch (IdentifierType.fromValue(identifierType)) {
        case BARCODE -> item.getBarcode();
        case HOLDINGS_RECORD_ID -> item.getHoldingsRecordId();
        case HRID -> item.getHrid();
        case FORMER_IDS -> String.join(ARRAY_DELIMITER, item.getFormerIds());
        case ACCESSION_NUMBER -> item.getAccessionNumber();
        default -> item.getId();
      };
    } catch (IllegalArgumentException e) {
      return item.getId();
    }
  }

  private String getHoldingsName(String holdingsId, String tenantId) {
    if (isEmpty(holdingsId)) {
      return EMPTY;
    }
    var holdingsJson = holdingsReferenceService.getHoldingsJsonById(holdingsId, tenantId);
    var locationId = isNull(holdingsJson.get(PERMANENT_LOCATION_ID)) ? null : holdingsJson.get(PERMANENT_LOCATION_ID).asText();

    var locationJson = holdingsReferenceService.getHoldingsLocationById(locationId, tenantId);
    var activePrefix = nonNull(locationJson.get(IS_ACTIVE)) && locationJson.get(IS_ACTIVE).asBoolean() ? EMPTY : "Inactive ";
    var name = isNull(locationJson.get(NAME)) ? EMPTY : locationJson.get(NAME).asText();
    var locationName = activePrefix + name;

    var callNumber = Stream.of(holdingsJson.get(CALL_NUMBER_PREFIX), holdingsJson.get(CALL_NUMBER), holdingsJson.get(CALL_NUMBER_SUFFIX))
      .filter(Objects::nonNull)
      .map(JsonNode::asText)
      .collect(Collectors.joining(SPACE));

    return String.join(HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER, locationName, callNumber);
  }
}
