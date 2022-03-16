package org.folio.dew.batch.bulkedit.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.CirculationNote;
import org.folio.dew.domain.dto.ContributorName;
import org.folio.dew.domain.dto.EffectiveCallNumberComponents;
import org.folio.dew.domain.dto.ElectronicAccess;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.domain.dto.ItemNote;
import org.folio.dew.domain.dto.LastCheckIn;
import org.folio.dew.domain.dto.StatisticalCode;
import org.folio.dew.domain.dto.Title;
import org.folio.dew.service.ItemReferenceService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemProcessor implements ItemProcessor<Item, ItemFormat> {
  private final ItemReferenceService itemReferenceService;

  @Override
  public ItemFormat process(Item item) {
    return ItemFormat.builder()
      .id(item.getId())
      .version(isNull(item.getVersion()) ? EMPTY : Integer.toString(item.getVersion()))
      .hrid(item.getHrid())
      .holdingsRecordId(item.getHoldingsRecordId())
      .formerIds(item.getFormerIds().isEmpty() ? EMPTY : String.join(ARRAY_DELIMITER, item.getFormerIds()))
      .discoverySuppress(isNull(item.getDiscoverySuppress()) ? EMPTY : item.getDiscoverySuppress().toString())
      .title(item.getTitle())
      .contributorNames(fetchContributorNames(item))
      .callNumber(item.getCallNumber())
      .barcode(item.getBarcode())
      .effectiveShelvingOrder(item.getEffectiveShelvingOrder())
      .accessionNumber(item.getAccessionNumber())
      .itemLevelCallNumber(item.getItemLevelCallNumber())
      .itemLevelCallNumberPrefix(item.getItemLevelCallNumberPrefix())
      .itemLevelCallNumberSuffix(item.getItemLevelCallNumberSuffix())
      .itemLevelCallNumberType(isEmpty(item.getItemLevelCallNumberTypeId()) ? EMPTY : itemReferenceService.getCallNumberTypeById(item.getItemLevelCallNumberTypeId()).getName())
      .effectiveCallNumberComponents(effectiveCallNumberComponentsToString(item.getEffectiveCallNumberComponents()))
      .volume(item.getVolume())
      .enumeration(item.getEnumeration())
      .chronology(item.getChronology())
      .yearCaption(item.getYearCaption().isEmpty() ? EMPTY : String.join(ARRAY_DELIMITER, item.getYearCaption()))
      .itemIdentifier(item.getItemIdentifier())
      .copyNumber(item.getCopyNumber())
      .numberOfPieces(item.getNumberOfPieces())
      .descriptionOfPieces(item.getDescriptionOfPieces())
      .numberOfMissingPieces(item.getNumberOfMissingPieces())
      .missingPieces(item.getMissingPieces())
      .missingPiecesDate(item.getMissingPiecesDate())
      .itemDamagedStatus(isEmpty(item.getItemDamagedStatusId()) ? EMPTY : itemReferenceService.getDamagedStatusById(item.getItemDamagedStatusId()).getName())
      .itemDamagedStatusDate(item.getItemDamagedStatusDate())
      .administrativeNotes(item.getAdministrativeNotes().isEmpty() ? EMPTY : String.join(ARRAY_DELIMITER, item.getAdministrativeNotes()))
      .notes(fetchNotes(item))
      .circulationNotes(fetchCirculationNotes(item))
      .status(String.join(ARRAY_DELIMITER, item.getStatus().getName().getValue(), dateToString(item.getStatus().getDate())))
      .materialType(item.getMaterialType().getName())
      .isBoundWith(item.getIsBoundWith().toString())
      .boundWithTitles(fetchBoundWithTitles(item))
      .permanentLoanType(isNull(item.getPermanentLoanType()) ? EMPTY : item.getPermanentLoanType().getName())
      .temporaryLoanType(isNull(item.getTemporaryLoanType()) ? EMPTY : item.getTemporaryLoanType().getName())
      .permanentLocation(isNull(item.getPermanentLocation()) ? EMPTY : item.getPermanentLocation().getName())
      .temporaryLocation(isNull(item.getTemporaryLocation()) ? EMPTY : item.getTemporaryLocation().getName())
      .effectiveLocation(isNull(item.getEffectiveLocation()) ? EMPTY : item.getEffectiveLocation().getName())
      .electronicAccess(fetchElectronicAccess(item))
      .inTransitDestinationServicePoint(isEmpty(item.getInTransitDestinationServicePointId()) ? EMPTY : itemReferenceService.getServicePointById(item.getInTransitDestinationServicePointId()).getName())
      .statisticalCodes(fetchStatisticalCodes(item))
      .purchaseOrderLineIdentifier(item.getPurchaseOrderLineIdentifier())
      .tags(isNull(item.getTags().getTagList()) ? EMPTY : String.join(ARRAY_DELIMITER, item.getTags().getTagList()))
      .lastCheckIn(lastCheckInToString(item.getLastCheckIn()))
      .build();
  }

  private String fetchContributorNames(Item item) {
    if (nonNull(item.getContributorNames())) {
      return item.getContributorNames().stream()
        .map(ContributorName::getName)
        .map(name -> name.replace(",", "/"))
        .collect(Collectors.joining(ARRAY_DELIMITER));
    }
    return EMPTY;
  }

  private String effectiveCallNumberComponentsToString(EffectiveCallNumberComponents components) {
    if (nonNull(components)) {
      return String.join(ARRAY_DELIMITER,
        components.getCallNumber(),
        components.getPrefix(),
        components.getSuffix(),
        itemReferenceService.getCallNumberTypeById(components.getTypeId()).getName());
    }
    return EMPTY;
  }

  private String fetchNotes(Item item) {
    if (nonNull(item.getNotes())) {
      return item.getNotes().stream()
        .map(this::itemNoteToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
    }
    return EMPTY;
  }

  private String itemNoteToString(ItemNote itemNote) {
    return String.join(ARRAY_DELIMITER,
      itemReferenceService.getNoteTypeById(itemNote.getItemNoteTypeId()).getName(),
      itemNote.getNote(),
      itemNote.getStaffOnly().toString());
  }

  private String fetchCirculationNotes(Item item) {
    if (nonNull(item.getCirculationNotes())) {
      return item.getCirculationNotes().stream()
        .map(this::circulationNotesToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
    }
    return EMPTY;
  }

  private String circulationNotesToString(CirculationNote note) {
    return String.join(ARRAY_DELIMITER,
      note.getId(),
      note.getNoteType().getValue(),
      note.getNote(),
      note.getStaffOnly().toString(),
      note.getSource().getId(),
      isEmpty(note.getSource().getPersonal().getLastName()) ? EMPTY : note.getSource().getPersonal().getLastName(),
      isEmpty(note.getSource().getPersonal().getFirstName()) ? EMPTY : note.getSource().getPersonal().getFirstName(),
      dateToString(note.getDate()));
  }

  private String fetchBoundWithTitles(Item item) {
    if (nonNull(item.getBoundWithTitles())) {
      return item.getBoundWithTitles().stream()
        .map(this::titleToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
    }
    return EMPTY;
  }

  private String titleToString(Title title) {
    return String.join(ARRAY_DELIMITER,
      title.getBriefHoldingsRecord().getHrid(),
      title.getBriefInstance().getHrid(),
      title.getBriefInstance().getTitle());
  }

  private String fetchElectronicAccess(Item item) {
    if (nonNull(item.getElectronicAccess())) {
      return item.getElectronicAccess().stream()
        .map(this::electronicAccessToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
    }
    return EMPTY;
  }

  private String electronicAccessToString(ElectronicAccess access) {
    return String.join(ARRAY_DELIMITER,
      access.getUri(),
      access.getLinkText(),
      access.getMaterialsSpecification(),
      access.getPublicNote(),
      isEmpty(access.getRelationshipId()) ? EMPTY : itemReferenceService.getRelationshipById(access.getRelationshipId()).getName());
  }

  private String fetchStatisticalCodes(Item item) {
    if (nonNull(item.getStatisticalCodeIds())) {
      return item.getStatisticalCodeIds().stream()
        .map(itemReferenceService::getStatisticalCodeById)
        .map(StatisticalCode::getCode)
        .collect(Collectors.joining(ARRAY_DELIMITER));
    }
    return EMPTY;
  }

  private String lastCheckInToString(LastCheckIn lastCheckIn) {
    if (nonNull(lastCheckIn)) {
      var servicePoint = itemReferenceService.getServicePointById(lastCheckIn.getServicePointId());
      var staffMember = itemReferenceService.getStaffMemberById(lastCheckIn.getStaffMemberId());
      return String.join(ARRAY_DELIMITER,
        isNull(servicePoint) ? EMPTY : servicePoint.getName(),
        isNull(staffMember) ? EMPTY : staffMember.getUsername(),
        lastCheckIn.getDateTime());
    }
    return EMPTY;
  }
}
