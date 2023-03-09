package org.folio.dew.service.mapper;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.service.mapper.MapperHelper.restoreStringValue;
import static org.folio.dew.utils.BulkEditProcessorHelper.booleanToStringNullSafe;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_PATTERN;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsNote;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.folio.dew.domain.dto.HoldingsStatement;
import org.folio.dew.domain.dto.ReceivingHistoryEntries;
import org.folio.dew.domain.dto.ReceivingHistoryEntry;
import org.folio.dew.domain.dto.Tags;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.ElectronicAccessService;
import org.folio.dew.service.HoldingsReferenceService;
import org.folio.dew.service.SpecialCharacterEscaper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class HoldingsMapper {
  private final HoldingsReferenceService holdingsReferenceService;
  private final ElectronicAccessService electronicAccessService;
  private final SpecialCharacterEscaper escaper;

  private static final int NUMBER_OF_HOLDINGS_NOTE_ELEMENTS = 3;
  private static final int HOLDINGS_NOTE_NOTE_TYPE_INDEX = 0;
  private static final int HOLDINGS_NOTE_NOTE_INDEX = 1;
  private static final int HOLDINGS_NOTE_STAFF_ONLY_INDEX = 2;

  private static final int NUMBER_OF_HOLDINGS_STATEMENT_ELEMENTS = 3;
  private static final int HOLDINGS_STATEMENT_STATEMENT_INDEX = 0;
  private static final int HOLDINGS_STATEMENT_NOTE_INDEX = 1;
  private static final int HOLDINGS_STATEMENT_STAFF_NOTE_INDEX = 2;

  private static final int NUMBER_OF_RECEIVING_HISTORY_ENTRY_ELEMENTS = 3;
  private static final int RECEIVING_HISTORY_ENTRY_PUBLIC_DISPLAY_INDEX = 0;
  private static final int RECEIVING_HISTORY_ENTRY_ENUMERATION_INDEX = 1;
  private static final int RECEIVING_HISTORY_ENTRY_CHRONOLOGY_INDEX = 2;



  public HoldingsFormat mapToHoldingsFormat(HoldingsRecord holdingsRecord, String identifier, String jobId, String errorFileName) {
    var errorServiceArgs = new ErrorServiceArgs(jobId, identifier, errorFileName);
    return HoldingsFormat.builder()
      .id(holdingsRecord.getId())
      .version(isEmpty(holdingsRecord.getVersion()) ? EMPTY : Integer.toString(holdingsRecord.getVersion()))
      .hrid(isEmpty(holdingsRecord.getHrid()) ? EMPTY : holdingsRecord.getHrid())
      .holdingsType(holdingsReferenceService.getHoldingsTypeNameById(holdingsRecord.getHoldingsTypeId(), errorServiceArgs))
      .formerIds(isEmpty(holdingsRecord.getFormerIds()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(holdingsRecord.getFormerIds())))
      .instance(isEmpty(holdingsRecord.getInstanceId()) ? EMPTY : String.join(ARRAY_DELIMITER, holdingsReferenceService.getInstanceTitleById(holdingsRecord.getInstanceId()), holdingsRecord.getInstanceId()))
      .permanentLocation(holdingsReferenceService.getLocationNameById(holdingsRecord.getPermanentLocationId()))
      .temporaryLocation(holdingsReferenceService.getLocationNameById(holdingsRecord.getTemporaryLocationId()))
      .effectiveLocation(holdingsReferenceService.getLocationNameById(holdingsRecord.getEffectiveLocationId()))
      .electronicAccess((electronicAccessService.getElectronicAccessesToString(holdingsRecord.getElectronicAccess(), identifier, jobId, errorFileName)))
      .callNumberType(holdingsReferenceService.getCallNumberTypeNameById(holdingsRecord.getCallNumberTypeId(), errorServiceArgs))
      .callNumberPrefix(isEmpty(holdingsRecord.getCallNumberPrefix()) ? EMPTY : holdingsRecord.getCallNumberPrefix())
      .callNumber(isEmpty(holdingsRecord.getCallNumber()) ? EMPTY : holdingsRecord.getCallNumber())
      .callNumberSuffix(isEmpty(holdingsRecord.getCallNumberSuffix()) ? EMPTY : holdingsRecord.getCallNumberSuffix())
      .shelvingTitle(isEmpty(holdingsRecord.getShelvingTitle()) ? EMPTY : holdingsRecord.getShelvingTitle())
      .acquisitionFormat(isEmpty(holdingsRecord.getAcquisitionFormat()) ? EMPTY : holdingsRecord.getAcquisitionFormat())
      .acquisitionMethod(isEmpty(holdingsRecord.getAcquisitionMethod()) ? EMPTY : holdingsRecord.getAcquisitionMethod())
      .receiptStatus(isEmpty(holdingsRecord.getReceiptStatus()) ? EMPTY : holdingsRecord.getReceiptStatus())
      .notes(notesToString(holdingsRecord.getNotes(), errorServiceArgs))
      .administrativeNotes(isEmpty(holdingsRecord.getAdministrativeNotes()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(holdingsRecord.getAdministrativeNotes())))
      .illPolicy(holdingsReferenceService.getIllPolicyNameById(holdingsRecord.getIllPolicyId(), errorServiceArgs))
      .retentionPolicy(isEmpty(holdingsRecord.getRetentionPolicy()) ? EMPTY : holdingsRecord.getRetentionPolicy())
      .digitizationPolicy(isEmpty(holdingsRecord.getDigitizationPolicy()) ? EMPTY : holdingsRecord.getDigitizationPolicy())
      .holdingsStatements(holdingsStatementsToString(holdingsRecord.getHoldingsStatements()))
      .holdingsStatementsForIndexes(holdingsStatementsToString(holdingsRecord.getHoldingsStatementsForIndexes()))
      .holdingsStatementsForSupplements(holdingsStatementsToString(holdingsRecord.getHoldingsStatementsForSupplements()))
      .copyNumber(isEmpty(holdingsRecord.getCopyNumber()) ? EMPTY : holdingsRecord.getCopyNumber())
      .numberOfItems(isEmpty(holdingsRecord.getNumberOfItems()) ? EMPTY : holdingsRecord.getNumberOfItems())
      .receivingHistory(receivingHistoryToString(holdingsRecord.getReceivingHistory()))
      .discoverySuppress(booleanToStringNullSafe(holdingsRecord.getDiscoverySuppress()))
      .statisticalCodes(getStatisticalCodeNames(holdingsRecord.getStatisticalCodeIds(), errorServiceArgs))
      .tags(tagsToString(holdingsRecord.getTags()))
      .source(holdingsReferenceService.getSourceNameById(holdingsRecord.getSourceId(), errorServiceArgs))
      .build();
  }

  private String notesToString(List<HoldingsNote> notes, ErrorServiceArgs args) {
    return isEmpty(notes) ? EMPTY : notes.stream()
      .filter(Objects::nonNull)
      .map(note -> String.join(ARRAY_DELIMITER,
        escaper.escape(holdingsReferenceService.getNoteTypeNameById(note.getHoldingsNoteTypeId(), args)),
        escaper.escape(note.getNote()),
        booleanToStringNullSafe(note.getStaffOnly())))
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String holdingsStatementsToString(List<HoldingsStatement> statements) {
    return isEmpty(statements) ? EMPTY : statements.stream()
      .filter(Objects::nonNull)
      .map(statement -> String.join(ARRAY_DELIMITER, escaper.escape(statement.getStatement()),
        escaper.escape(statement.getNote()), escaper.escape(statement.getStaffNote())))
      .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String receivingHistoryToString(ReceivingHistoryEntries receivingHistoryEntries) {
    if (isEmpty(receivingHistoryEntries)) {
      return EMPTY;
    }
    var displayType = isEmpty(receivingHistoryEntries.getDisplayType()) ? EMPTY : receivingHistoryEntries.getDisplayType();
    var entriesString = isEmpty(receivingHistoryEntries.getEntries()) ? EMPTY : receivingHistoryEntries.getEntries().stream()
      .filter(Objects::nonNull)
      .map(this::receivingHistoryEntryToString)
      .collect(Collectors.joining(ITEM_DELIMITER));
    return String.join(ITEM_DELIMITER, displayType, entriesString);
  }

  private String receivingHistoryEntryToString(ReceivingHistoryEntry entry) {
    return String.join(ARRAY_DELIMITER,
      booleanToStringNullSafe(entry.getPublicDisplay()),
      isEmpty(entry.getEnumeration()) ? EMPTY : escaper.escape(entry.getEnumeration()),
      isEmpty(entry.getChronology()) ? EMPTY : escaper.escape(entry.getChronology()));
  }

  private String getStatisticalCodeNames(List<String> codeIds, ErrorServiceArgs args) {
    return isEmpty(codeIds) ? EMPTY : codeIds.stream()
      .filter(Objects::nonNull)
      .map(id -> holdingsReferenceService.getStatisticalCodeNameById(id, args))
      .map(escaper::escape)
      .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private String tagsToString(Tags tags) {
    if (isEmpty(tags)) {
      return EMPTY;
    }
    return isEmpty(tags.getTagList()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(tags.getTagList()));
  }

  public HoldingsRecord mapToHoldingsRecord(HoldingsFormat holdingsFormat) {
    return new HoldingsRecord()
      .id(holdingsFormat.getId())
      .version(isEmpty(holdingsFormat.getVersion()) ? null : Integer.parseInt(holdingsFormat.getVersion()))
      .hrid(holdingsFormat.getHrid())
      .holdingsTypeId(holdingsReferenceService.getHoldingsTypeIdByName(holdingsFormat.getHoldingsType()))
      .formerIds(restoreListValue(holdingsFormat.getFormerIds()))
      .instanceId(restoreInstanceId(holdingsFormat.getInstance()))
      .permanentLocationId(holdingsReferenceService.getLocationByName(holdingsFormat.getPermanentLocation()).getId())
      .temporaryLocationId(isEmpty(holdingsFormat.getTemporaryLocation()) ? null : holdingsReferenceService.getLocationByName(holdingsFormat.getTemporaryLocation()).getId())
      .effectiveLocationId(isEmpty(holdingsFormat.getEffectiveLocation()) ? null : holdingsReferenceService.getLocationByName(holdingsFormat.getEffectiveLocation()).getId())
      .electronicAccess(isEmpty(holdingsFormat.getElectronicAccess()) ? Collections.emptyList() : electronicAccessService.restoreElectronicAccess(holdingsFormat.getElectronicAccess()))
      .callNumberTypeId(isEmpty(holdingsFormat.getCallNumberType()) ? null : holdingsReferenceService.getCallNumberTypeIdByName(holdingsFormat.getCallNumberType()))
      .callNumberPrefix(restoreStringValue(holdingsFormat.getCallNumberPrefix()))
      .callNumber(restoreStringValue(holdingsFormat.getCallNumber()))
      .callNumberSuffix(restoreStringValue(holdingsFormat.getCallNumberSuffix()))
      .shelvingTitle(restoreStringValue(holdingsFormat.getShelvingTitle()))
      .acquisitionFormat(restoreStringValue(holdingsFormat.getAcquisitionFormat()))
      .acquisitionMethod(restoreStringValue(holdingsFormat.getAcquisitionMethod()))
      .receiptStatus(restoreStringValue(holdingsFormat.getReceiptStatus()))
      .administrativeNotes(restoreListValue(holdingsFormat.getAdministrativeNotes()))
      .notes(restoreHoldingsNotes(holdingsFormat.getNotes()))
      .illPolicyId(holdingsReferenceService.getIllPolicyIdByName(holdingsFormat.getIllPolicy()))
      .retentionPolicy(restoreStringValue(holdingsFormat.getRetentionPolicy()))
      .digitizationPolicy(restoreStringValue(holdingsFormat.getDigitizationPolicy()))
      .holdingsStatements(restoreHoldingsStatements(holdingsFormat.getHoldingsStatements()))
      .holdingsStatementsForIndexes(restoreHoldingsStatements(holdingsFormat.getHoldingsStatementsForIndexes()))
      .holdingsStatementsForSupplements(restoreHoldingsStatements(holdingsFormat.getHoldingsStatementsForSupplements()))
      .copyNumber(restoreStringValue(holdingsFormat.getCopyNumber()))
      .numberOfItems(restoreStringValue(holdingsFormat.getNumberOfItems()))
      .receivingHistory(restoreReceivingHistory(holdingsFormat.getReceivingHistory()))
      .discoverySuppress(isEmpty(holdingsFormat.getDiscoverySuppress()) ? null : Boolean.parseBoolean(holdingsFormat.getDiscoverySuppress()))
      .statisticalCodeIds(restoreStatisticalCodeIds(holdingsFormat.getStatisticalCodes()))
      .tags(isEmpty(holdingsFormat.getTags()) ? null : new Tags().tagList(restoreListValue(holdingsFormat.getTags())))
      .sourceId(isEmpty(holdingsFormat.getSource()) ? null : holdingsReferenceService.getSourceIdByName(holdingsFormat.getSource()));
  }

  private String restoreInstanceId(String instanceString) {
    var tokens = instanceString.split(ARRAY_DELIMITER);
    return tokens[tokens.length - 1];
  }

  private List<HoldingsNote> restoreHoldingsNotes(String notesString) {
    return isEmpty(notesString) ?
      Collections.emptyList() :
      Arrays.stream(notesString.split(ITEM_DELIMITER_PATTERN))
        .map(this::restoreHoldingsNote)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private HoldingsNote restoreHoldingsNote(String noteString) {
    if (isEmpty(noteString)) {
      return null;
    }
    var tokens = noteString.split(ARRAY_DELIMITER, -1);
    if (tokens.length < NUMBER_OF_HOLDINGS_NOTE_ELEMENTS) {
      throw new BulkEditException(String.format("Illegal number of holdings note elements: %d, expected: %d", tokens.length,
        NUMBER_OF_HOLDINGS_NOTE_ELEMENTS));
    }
    return new HoldingsNote()
      .holdingsNoteTypeId(holdingsReferenceService.getNoteTypeIdByName(escaper.restore(tokens[HOLDINGS_NOTE_NOTE_TYPE_INDEX])))
      .note(escaper.restore(tokens[HOLDINGS_NOTE_NOTE_INDEX]))
      .staffOnly(isEmpty(tokens[HOLDINGS_NOTE_STAFF_ONLY_INDEX]) ? null : Boolean.parseBoolean(tokens[HOLDINGS_NOTE_STAFF_ONLY_INDEX]));
  }

  private List<HoldingsStatement> restoreHoldingsStatements(String statements) {
    return isEmpty(statements) ?
      Collections.emptyList() :
      Arrays.stream(statements.split(ITEM_DELIMITER_PATTERN))
        .map(this::restoreHoldingsStatement)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private HoldingsStatement restoreHoldingsStatement(String statementString) {
    if (isEmpty(statementString)) {
      return null;
    }
    var tokens = statementString.split(ARRAY_DELIMITER, -1);
    if (tokens.length < NUMBER_OF_HOLDINGS_STATEMENT_ELEMENTS) {
      throw new BulkEditException(String.format("Illegal number of holdings statement elements: %d, expected: %d", tokens.length, NUMBER_OF_HOLDINGS_STATEMENT_ELEMENTS));
    }
    return new HoldingsStatement()
      .statement(escaper.restore(tokens[HOLDINGS_STATEMENT_STATEMENT_INDEX]))
      .note(escaper.restore(tokens[HOLDINGS_STATEMENT_NOTE_INDEX]))
      .staffNote(escaper.restore(tokens[HOLDINGS_STATEMENT_STAFF_NOTE_INDEX]));
  }

  private ReceivingHistoryEntries restoreReceivingHistory(String historyString) {
    if (isEmpty(historyString)) {
      return null;
    }
    var tokens = historyString.split(ITEM_DELIMITER_PATTERN);
    if (tokens.length > 1) {
      return new ReceivingHistoryEntries()
        .displayType(isEmpty(tokens[0]) ? null : tokens[0])
        .entries(Arrays.stream(tokens)
          .skip(1)
          .map(this::restoreReceivingHistoryEntry)
          .collect(Collectors.toList()));
    }
    throw new BulkEditException("Invalid number of tokens in receiving history entries");
  }

  private ReceivingHistoryEntry restoreReceivingHistoryEntry(String entryString) {
    var tokens = entryString.split(ARRAY_DELIMITER);
    if (tokens.length == NUMBER_OF_RECEIVING_HISTORY_ENTRY_ELEMENTS) {
      return new ReceivingHistoryEntry()
        .publicDisplay(isEmpty(tokens[RECEIVING_HISTORY_ENTRY_PUBLIC_DISPLAY_INDEX]) ? null : Boolean.parseBoolean(tokens[RECEIVING_HISTORY_ENTRY_PUBLIC_DISPLAY_INDEX]))
        .enumeration(escaper.restore(tokens[RECEIVING_HISTORY_ENTRY_ENUMERATION_INDEX]))
        .chronology(escaper.restore(tokens[RECEIVING_HISTORY_ENTRY_CHRONOLOGY_INDEX]));
    }
    throw new BulkEditException(String.format("Invalid number of tokens in receiving history entry: %d, expected %d", tokens.length, NUMBER_OF_RECEIVING_HISTORY_ENTRY_ELEMENTS));
  }

  private List<String> restoreStatisticalCodeIds(String codesString) {
    return isEmpty(codesString) ?
      Collections.emptyList() :
      Arrays.stream(codesString.split(ARRAY_DELIMITER))
        .map(escaper::restore)
        .map(holdingsReferenceService::getStatisticalCodeIdByName)
        .collect(Collectors.toList());
  }

  private List<String> restoreListValue(String s) {
    return StringUtils.isEmpty(s) ?
      Collections.emptyList() :
      escaper.restore(Arrays.asList(s.split(ARRAY_DELIMITER)));
  }
}
