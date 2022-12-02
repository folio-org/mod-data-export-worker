package org.folio.dew.batch.bulkedit.jobs;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
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
import org.folio.dew.domain.dto.Title;
import org.folio.dew.service.ElectronicAccessService;
import org.folio.dew.service.ItemReferenceService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemProcessor implements ItemProcessor<Item, ItemFormat> {
  private final ItemReferenceService itemReferenceService;
  private final ElectronicAccessService electronicAccessService;

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
      .formerIds(isEmpty(item.getFormerIds()) ? EMPTY : String.join(ARRAY_DELIMITER, item.getFormerIds()))
      .discoverySuppress(isEmpty(item.getDiscoverySuppress()) ? EMPTY : item.getDiscoverySuppress().toString())
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
      .effectiveCallNumberComponents(effectiveCallNumberComponentsToString(item.getEffectiveCallNumberComponents(), errorServiceArgs))
      .volume(item.getVolume())
      .enumeration(item.getEnumeration())
      .chronology(item.getChronology())
      .yearCaption(isEmpty(item.getYearCaption()) ? EMPTY : String.join(ARRAY_DELIMITER, item.getYearCaption()))
      .itemIdentifier(item.getItemIdentifier())
      .copyNumber(item.getCopyNumber())
      .numberOfPieces(item.getNumberOfPieces())
      .descriptionOfPieces(item.getDescriptionOfPieces())
      .numberOfMissingPieces(item.getNumberOfMissingPieces())
      .missingPieces(item.getMissingPieces())
      .missingPiecesDate(item.getMissingPiecesDate())
      .itemDamagedStatus(itemReferenceService.getDamagedStatusNameById(item.getItemDamagedStatusId(), errorServiceArgs))
      .itemDamagedStatusDate(item.getItemDamagedStatusDate())
      .administrativeNotes(isEmpty(item.getAdministrativeNotes()) ? EMPTY : String.join(ARRAY_DELIMITER, item.getAdministrativeNotes()))
      .notes(fetchNotes(item, errorServiceArgs))
      .circulationNotes(fetchCirculationNotes(item))
      .status(String.join(ARRAY_DELIMITER, item.getStatus().getName().getValue(), dateToString(item.getStatus().getDate())))
      .materialType(item.getMaterialType().getName())
      .isBoundWith(item.getIsBoundWith().toString())
      .boundWithTitles(fetchBoundWithTitles(item))
      .permanentLoanType(isEmpty(item.getPermanentLoanType()) ? EMPTY : item.getPermanentLoanType().getName())
      .temporaryLoanType(isEmpty(item.getTemporaryLoanType()) ? EMPTY : item.getTemporaryLoanType().getName())
      .permanentLocation(isEmpty(item.getPermanentLocation()) ? EMPTY : item.getPermanentLocation().getName())
      .temporaryLocation(isEmpty(item.getTemporaryLocation()) ? EMPTY : item.getTemporaryLocation().getName())
      .effectiveLocation(isEmpty(item.getEffectiveLocation()) ? EMPTY : item.getEffectiveLocation().getName())
      .inTransitDestinationServicePoint(itemReferenceService.getServicePointNameById(item.getInTransitDestinationServicePointId(), errorServiceArgs))
      .statisticalCodes(fetchStatisticalCodes(item, errorServiceArgs))
      .purchaseOrderLineIdentifier(item.getPurchaseOrderLineIdentifier())
      .tags(isEmpty(item.getTags().getTagList()) ? EMPTY : String.join(ARRAY_DELIMITER, item.getTags().getTagList()))
      .lastCheckIn(lastCheckInToString(item, errorServiceArgs))
      .build();
    itemFormat.setElectronicAccess(electronicAccessService.getElectronicAccessesToString(item.getElectronicAccess(), itemFormat.getIdentifier(identifierType), jobId, FilenameUtils.getName(fileName)));
    return itemFormat;
  }

  private String fetchContributorNames(Item item) {
    return isEmpty(item.getContributorNames()) ?
      EMPTY :
      item.getContributorNames().stream()
        .map(ContributorName::getName)
        .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private String effectiveCallNumberComponentsToString(EffectiveCallNumberComponents components, ErrorServiceArgs args) {
    if (isEmpty(components)) {
      return EMPTY;
    }
    return String.join(ARRAY_DELIMITER,
      isEmpty(components.getCallNumber()) ? EMPTY : components.getCallNumber(),
      isEmpty(components.getPrefix()) ? EMPTY : components.getPrefix(),
      isEmpty(components.getSuffix()) ? EMPTY : components.getSuffix(),
      itemReferenceService.getCallNumberTypeNameById(components.getTypeId(), args));
  }

  private String fetchNotes(Item item, ErrorServiceArgs args) {
    return isEmpty(item.getNotes()) ?
      EMPTY :
      item.getNotes().stream()
        .map(itemNote -> String.join(ARRAY_DELIMITER,
          itemReferenceService.getNoteTypeNameById(itemNote.getItemNoteTypeId(), args),
          itemNote.getNote(),
          itemNote.getStaffOnly().toString()))
        .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String fetchCirculationNotes(Item item) {
    return isEmpty(item.getCirculationNotes()) ?
      EMPTY :
      item.getCirculationNotes().stream()
        .map(this::circulationNotesToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String circulationNotesToString(CirculationNote note) {
    return String.join(ARRAY_DELIMITER,
      note.getId(),
      note.getNoteType().getValue(),
      note.getNote(),
      note.getStaffOnly().toString(),
      isEmpty(note.getSource().getId()) ? EMPTY : note.getSource().getId(),
      isEmpty(note.getSource().getPersonal().getLastName()) ? EMPTY : note.getSource().getPersonal().getLastName(),
      isEmpty(note.getSource().getPersonal().getFirstName()) ? EMPTY : note.getSource().getPersonal().getFirstName(),
      dateToString(note.getDate()));
  }

  private String fetchBoundWithTitles(Item item) {
    return isEmpty(item.getBoundWithTitles()) ?
      EMPTY :
      item.getBoundWithTitles().stream()
        .map(this::titleToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String titleToString(Title title) {
    return String.join(ARRAY_DELIMITER,
      title.getBriefHoldingsRecord().getHrid(),
      title.getBriefInstance().getHrid(),
      title.getBriefInstance().getTitle());
  }

  private String fetchStatisticalCodes(Item item, ErrorServiceArgs args) {
    return isEmpty(item.getStatisticalCodeIds()) ?
      EMPTY :
      item.getStatisticalCodeIds().stream()
        .map(id -> itemReferenceService.getStatisticalCodeById(id, args))
        .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private String lastCheckInToString(Item item, ErrorServiceArgs args) {
    var lastCheckIn = item.getLastCheckIn();
    if (isEmpty(lastCheckIn)) {
      return EMPTY;
    }
    return String.join(ARRAY_DELIMITER,
      itemReferenceService.getServicePointNameById(lastCheckIn.getServicePointId(), args),
      itemReferenceService.getUserNameById(lastCheckIn.getStaffMemberId(), args),
      lastCheckIn.getDateTime());
  }

  private String getIdentifier(Item item, String identifierType) {
    try {
      switch (IdentifierType.fromValue(identifierType)) {
      case BARCODE:
        return item.getBarcode();
      case HOLDINGS_RECORD_ID:
        return item.getHoldingsRecordId();
      case HRID:
        return item.getHrid();
      case FORMER_IDS:
        return String.join(ARRAY_DELIMITER, item.getFormerIds());
      case ACCESSION_NUMBER:
        return item.getAccessionNumber();
      default:
        return item.getId();
      }
    } catch (IllegalArgumentException e) {
      return item.getId();
    }
  }
}
