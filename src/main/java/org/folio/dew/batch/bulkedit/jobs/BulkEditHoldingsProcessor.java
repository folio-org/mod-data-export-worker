package org.folio.dew.batch.bulkedit.jobs;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.IdentifierType.INSTANCE_HRID;
import static org.folio.dew.domain.dto.IdentifierType.ITEM_BARCODE;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsNote;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.folio.dew.domain.dto.HoldingsRecordCollection;
import org.folio.dew.domain.dto.HoldingsStatement;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.ReceivingHistoryEntries;
import org.folio.dew.domain.dto.ReceivingHistoryEntry;
import org.folio.dew.domain.dto.Tags;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.ElectronicAccessService;
import org.folio.dew.service.HoldingsReferenceService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditHoldingsProcessor implements ItemProcessor<ItemIdentifier, List<HoldingsFormat>> {
  private final HoldingClient holdingClient;
  private final HoldingsReferenceService holdingsReferenceService;
  private final ElectronicAccessService electronicAccessService;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  private Set<ItemIdentifier> identifiersToCheckDuplication = new HashSet<>();

  @Override
  public List<HoldingsFormat> process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry");
    }
    identifiersToCheckDuplication.add(itemIdentifier);

    var holdings = getHoldingsRecords(itemIdentifier);
    if (holdings.getHoldingsRecords().isEmpty()) {
      log.error(NO_MATCH_FOUND_MESSAGE);
      throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
    }

    var instanceHrid = INSTANCE_HRID == IdentifierType.fromValue(identifierType) ? itemIdentifier.getItemId() : null;
    var itemBarcode = ITEM_BARCODE == IdentifierType.fromValue(identifierType) ? itemIdentifier.getItemId() : null;

    return holdings.getHoldingsRecords().stream()
      .map(this::mapHoldingsRecordToHoldingsFormat)
      .map(holdingsFormat -> holdingsFormat.withInstanceHrid(instanceHrid))
      .map(holdingsFormat -> holdingsFormat.withItemBarcode(itemBarcode))
      .collect(Collectors.toList());
  }

  private HoldingsRecordCollection getHoldingsRecords(ItemIdentifier itemIdentifier) {
    switch (IdentifierType.fromValue(identifierType)) {
      case ID:
      case HRID:
        return holdingClient.getHoldingsByQuery(String.format(getMatchPattern(identifierType), resolveIdentifier(identifierType), itemIdentifier.getItemId()));
      case INSTANCE_HRID:
        return holdingClient.getHoldingsByQuery("instanceId==" + holdingsReferenceService.getInstanceIdByHrid(itemIdentifier.getItemId()));
      case ITEM_BARCODE:
        return holdingClient.getHoldingsByQuery("id==" + holdingsReferenceService.getHoldingsIdByItemBarcode(itemIdentifier.getItemId()), 1);
      default:
        throw new BulkEditException(String.format("Identifier type \"%s\" is not supported", identifierType));
    }
  }

  private HoldingsFormat mapHoldingsRecordToHoldingsFormat(HoldingsRecord holdingsRecord) {
    return HoldingsFormat.builder()
      .id(holdingsRecord.getId())
      .version(isEmpty(holdingsRecord.getVersion()) ? EMPTY : Integer.toString(holdingsRecord.getVersion()))
      .hrid(isEmpty(holdingsRecord.getHrid()) ? EMPTY : holdingsRecord.getHrid())
      .holdingsType(holdingsReferenceService.getHoldingsTypeNameById(holdingsRecord.getHoldingsTypeId()))
      .formerIds(isEmpty(holdingsRecord.getFormerIds()) ? EMPTY : String.join(ARRAY_DELIMITER, holdingsRecord.getFormerIds()))
      .instanceId(isEmpty(holdingsRecord.getInstanceId()) ? EMPTY : holdingsRecord.getInstanceId())
      .permanentLocation(holdingsReferenceService.getLocationNameById(holdingsRecord.getPermanentLocationId()))
      .temporaryLocation(holdingsReferenceService.getLocationNameById(holdingsRecord.getTemporaryLocationId()))
      .effectiveLocation(holdingsReferenceService.getLocationNameById(holdingsRecord.getEffectiveLocationId()))
      .electronicAccess(electronicAccessService.electronicAccessesToString(holdingsRecord.getElectronicAccess()))
      .callNumberType(holdingsReferenceService.getCallNumberTypeNameById(holdingsRecord.getCallNumberTypeId()))
      .callNumberPrefix(isEmpty(holdingsRecord.getCallNumberPrefix()) ? EMPTY : holdingsRecord.getCallNumberPrefix())
      .callNumber(isEmpty(holdingsRecord.getCallNumber()) ? EMPTY : holdingsRecord.getCallNumber())
      .callNumberSuffix(isEmpty(holdingsRecord.getCallNumberSuffix()) ? EMPTY : holdingsRecord.getCallNumberSuffix())
      .shelvingTitle(isEmpty(holdingsRecord.getShelvingTitle()) ? EMPTY : holdingsRecord.getShelvingTitle())
      .acquisitionFormat(isEmpty(holdingsRecord.getAcquisitionFormat()) ? EMPTY : holdingsRecord.getAcquisitionFormat())
      .acquisitionMethod(isEmpty(holdingsRecord.getAcquisitionMethod()) ? EMPTY : holdingsRecord.getAcquisitionMethod())
      .receiptStatus(isEmpty(holdingsRecord.getReceiptStatus()) ? EMPTY : holdingsRecord.getReceiptStatus())
      .notes(notesToString(holdingsRecord.getNotes()))
      .administrativeNotes(isEmpty(holdingsRecord.getAdministrativeNotes()) ? EMPTY : String.join(ARRAY_DELIMITER, holdingsRecord.getAdministrativeNotes()))
      .illPolicy(holdingsReferenceService.getIllPolicyNameById(holdingsRecord.getIllPolicyId()))
      .retentionPolicy(isEmpty(holdingsRecord.getRetentionPolicy()) ? EMPTY : holdingsRecord.getRetentionPolicy())
      .holdingsStatements(holdingsStatementsToString(holdingsRecord.getHoldingsStatements()))
      .holdingsStatementsForIndexes(holdingsStatementsToString(holdingsRecord.getHoldingsStatementsForIndexes()))
      .holdingsStatementsForSupplements(holdingsStatementsToString(holdingsRecord.getHoldingsStatementsForSupplements()))
      .copyNumber(isEmpty(holdingsRecord.getCopyNumber()) ? EMPTY : holdingsRecord.getCopyNumber())
      .numberOfItems(isEmpty(holdingsRecord.getNumberOfItems()) ? EMPTY : holdingsRecord.getNumberOfItems())
      .receivingHistory(receivingHistoryToString(holdingsRecord.getReceivingHistory()))
      .discoverySuppress(isEmpty(holdingsRecord.getDiscoverySuppress()) ? EMPTY : Boolean.toString(holdingsRecord.getDiscoverySuppress()))
      .statisticalCodes(getStatisticalCodeNames(holdingsRecord.getStatisticalCodeIds()))
      .tags(tagsToString(holdingsRecord.getTags()))
      .source(holdingsReferenceService.getSourceNameById(holdingsRecord.getSourceId()))
      .build();
  }

  private String notesToString(List<HoldingsNote> notes) {
    return isEmpty(notes) ? EMPTY : notes.stream()
      .map(note -> String.join(ARRAY_DELIMITER,
        holdingsReferenceService.getNoteTypeNameById(note.getHoldingsNoteTypeId()),
        note.getNote(),
        Boolean.toString(note.getStaffOnly())))
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String holdingsStatementsToString(List<HoldingsStatement> statements) {
    return isEmpty(statements) ? EMPTY : statements.stream()
      .map(statement -> String.join(ARRAY_DELIMITER, statement.getStatement(), statement.getNote(), statement.getStaffNote()))
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String receivingHistoryToString(ReceivingHistoryEntries receivingHistoryEntries) {
    if (isEmpty(receivingHistoryEntries)) {
      return EMPTY;
    }
    var displayType = isEmpty(receivingHistoryEntries.getDisplayType()) ? EMPTY : receivingHistoryEntries.getDisplayType();
    var entriesString = isEmpty(receivingHistoryEntries.getEntries()) ? EMPTY : receivingHistoryEntries.getEntries().stream()
      .map(this::receivingHistoryEntryToString)
      .collect(Collectors.joining(ITEM_DELIMITER));
    return String.join(ITEM_DELIMITER, displayType, entriesString);
  }

  private String receivingHistoryEntryToString(ReceivingHistoryEntry entry) {
    return String.join(ARRAY_DELIMITER,
      isEmpty(entry.getPublicDisplay()) ? EMPTY : Boolean.toString(entry.getPublicDisplay()),
      isEmpty(entry.getEnumeration()) ? EMPTY : entry.getEnumeration(),
      isEmpty(entry.getChronology()) ? EMPTY : entry.getChronology());
  }

  private String getStatisticalCodeNames(List<String> codeIds) {
    return isEmpty(codeIds) ? EMPTY : codeIds.stream()
      .map(holdingsReferenceService::getStatisticalCodeNameById)
      .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private String tagsToString(Tags tags) {
    if (isEmpty(tags)) {
      return EMPTY;
    }
    return isEmpty(tags.getTagList()) ? EMPTY : String.join(ARRAY_DELIMITER, tags.getTagList());
  }
}
