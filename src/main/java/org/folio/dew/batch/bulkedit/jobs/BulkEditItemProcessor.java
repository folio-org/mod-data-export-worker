package org.folio.dew.batch.bulkedit.jobs;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.dew.utils.BulkEditProcessorHelper.booleanToStringNullSafe;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
import static org.folio.dew.utils.BulkEditProcessorHelper.ofEmptyString;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.dto.CirculationNote;
import org.folio.dew.domain.dto.ContributorName;
import org.folio.dew.domain.dto.EffectiveCallNumberComponents;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.domain.dto.Source;
import org.folio.dew.domain.dto.Title;
import org.folio.dew.service.ElectronicAccessService;
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

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemProcessor implements ItemProcessor<Item, ItemFormat> {
  private final ItemReferenceService itemReferenceService;
  private final ElectronicAccessService electronicAccessService;
  private final SpecialCharacterEscaper escaper;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;

  @Override
  public ItemFormat process(Item item) {
    var errorServiceArgs = new ErrorServiceArgs(jobId, getIdentifier(item, identifierType), FilenameUtils.getName(fileName));
    var itemFormat = ItemFormat.builder()
      .id(item.getId())
      .version(isEmpty(item.getVersion()) ? EMPTY : Integer.toString(item.getVersion()))
      .hrid(item.getHrid())
      .holdingsRecordId(item.getHoldingsRecordId())
      .formerIds(isEmpty(item.getFormerIds()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(item.getFormerIds())))
      .discoverySuppress(booleanToStringNullSafe(item.getDiscoverySuppress()))
      .title(item.getTitle())
      .contributorNames(fetchContributorNames(item))
      .callNumber(item.getCallNumber())
      .barcode(item.getBarcode())
      .effectiveShelvingOrder(item.getEffectiveShelvingOrder())
      .accessionNumber(item.getAccessionNumber())
      .itemLevelCallNumber(item.getItemLevelCallNumber())
      .itemLevelCallNumberPrefix(item.getItemLevelCallNumberPrefix())
      .itemLevelCallNumberSuffix(item.getItemLevelCallNumberSuffix())
      .itemLevelCallNumberType(itemReferenceService.getCallNumberTypeNameById(item.getItemLevelCallNumberTypeId(), errorServiceArgs))
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
      .itemDamagedStatus(itemReferenceService.getDamagedStatusNameById(item.getItemDamagedStatusId(), errorServiceArgs))
      .itemDamagedStatusDate(item.getItemDamagedStatusDate())
      .administrativeNotes(isEmpty(item.getAdministrativeNotes()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(item.getAdministrativeNotes())))
      .notes(fetchNotes(item, errorServiceArgs))
      .circulationNotes(fetchCirculationNotes(item))
      .status(statusToString(item))
      .materialType(isEmpty(item.getMaterialType()) ? EMPTY : item.getMaterialType().getName())
      .isBoundWith(booleanToStringNullSafe(item.getIsBoundWith()))
      .boundWithTitles(fetchBoundWithTitles(item))
      .permanentLoanType(isEmpty(item.getPermanentLoanType()) ? EMPTY : item.getPermanentLoanType().getName())
      .temporaryLoanType(isEmpty(item.getTemporaryLoanType()) ? EMPTY : item.getTemporaryLoanType().getName())
      .permanentLocation(isEmpty(item.getPermanentLocation()) ? EMPTY : item.getPermanentLocation().getName())
      .temporaryLocation(isEmpty(item.getTemporaryLocation()) ? EMPTY : item.getTemporaryLocation().getName())
      .effectiveLocation(isEmpty(item.getEffectiveLocation()) ? EMPTY : item.getEffectiveLocation().getName())
      .inTransitDestinationServicePoint(itemReferenceService.getServicePointNameById(item.getInTransitDestinationServicePointId(), errorServiceArgs))
      .statisticalCodes(fetchStatisticalCodes(item, errorServiceArgs))
      .purchaseOrderLineIdentifier(item.getPurchaseOrderLineIdentifier())
      .tags(isEmpty(item.getTags()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(item.getTags().getTagList())))
      .lastCheckIn(lastCheckInToString(item, errorServiceArgs))
      .build();
    itemFormat.setElectronicAccess(electronicAccessService.getElectronicAccessesToString(item.getElectronicAccess(), itemFormat.getIdentifier(identifierType), jobId, FilenameUtils.getName(fileName)));
    return itemFormat.withOriginal(item);
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

  private String fetchCirculationNotes(Item item) {
    return isEmpty(item.getCirculationNotes()) ?
      EMPTY :
      item.getCirculationNotes().stream()
        .filter(Objects::nonNull)
        .map(this::circulationNotesToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String circulationNotesToString(CirculationNote note) {
    var source = isEmpty(note.getSource()) ? new Source() : note.getSource();
    return String.join(ARRAY_DELIMITER,
      note.getId(),
      isEmpty(note.getNoteType()) ? EMPTY : note.getNoteType().getValue(),
      escaper.escape(note.getNote()),
      booleanToStringNullSafe(note.getStaffOnly()),
      isEmpty(source.getId()) ? EMPTY : note.getSource().getId(),
      isEmpty(source.getPersonal()) ? EMPTY : escaper.escape(source.getPersonal().getLastName()),
      isEmpty(source.getPersonal()) ? EMPTY : escaper.escape(source.getPersonal().getFirstName()),
      dateToString(note.getDate()));
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

  private String lastCheckInToString(Item item, ErrorServiceArgs args) {
    var lastCheckIn = item.getLastCheckIn();
    if (isEmpty(lastCheckIn)) {
      return EMPTY;
    }
    return String.join(ARRAY_DELIMITER,
      escaper.escape(itemReferenceService.getServicePointNameById(lastCheckIn.getServicePointId(), args)),
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
}
