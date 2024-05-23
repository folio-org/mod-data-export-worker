package org.folio.dew.service.mapper;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.dew.utils.BulkEditProcessorHelper.booleanToStringNullSafe;
import static org.folio.dew.utils.Constants.ACTION_NOTE;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.BINDING;
import static org.folio.dew.utils.Constants.COPY_NOTE;
import static org.folio.dew.utils.Constants.ELECTRONIC_BOOKPLATE;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_PATTERN;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_SPACED;
import static org.folio.dew.utils.Constants.NOTE;
import static org.folio.dew.utils.Constants.PROVENANCE;
import static org.folio.dew.utils.Constants.REPRODUCTION;
import static org.folio.dew.utils.Constants.STAFF_ONLY;

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
      .instanceTitle(isEmpty(holdingsRecord.getInstanceId()) ? EMPTY : holdingsReferenceService.getInstanceTitleById(holdingsRecord.getInstanceId()))
      .discoverySuppress(booleanToStringNullSafe(holdingsRecord.getDiscoverySuppress()))
      .hrid(isEmpty(holdingsRecord.getHrid()) ? EMPTY : holdingsRecord.getHrid())
      .source(holdingsReferenceService.getSourceNameById(holdingsRecord.getSourceId(), errorServiceArgs))
      .formerIds(isEmpty(holdingsRecord.getFormerIds()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(holdingsRecord.getFormerIds())))
      .holdingsType(holdingsReferenceService.getHoldingsTypeNameById(holdingsRecord.getHoldingsTypeId(), errorServiceArgs))
      .statisticalCodes(getStatisticalCodeNames(holdingsRecord.getStatisticalCodeIds(), errorServiceArgs))
      .administrativeNotes(isEmpty(holdingsRecord.getAdministrativeNotes()) ? EMPTY : String.join(ARRAY_DELIMITER, escaper.escape(holdingsRecord.getAdministrativeNotes())))
      .permanentLocation(holdingsReferenceService.getLocationNameById(holdingsRecord.getPermanentLocationId()))
      .temporaryLocation(holdingsReferenceService.getLocationNameById(holdingsRecord.getTemporaryLocationId()))
      .shelvingTitle(isEmpty(holdingsRecord.getShelvingTitle()) ? EMPTY : holdingsRecord.getShelvingTitle())
      .copyNumber(isEmpty(holdingsRecord.getCopyNumber()) ? EMPTY : holdingsRecord.getCopyNumber())
      .callNumberType(holdingsReferenceService.getCallNumberTypeNameById(holdingsRecord.getCallNumberTypeId(), errorServiceArgs))
      .callNumberPrefix(isEmpty(holdingsRecord.getCallNumberPrefix()) ? EMPTY : holdingsRecord.getCallNumberPrefix())
      .callNumber(isEmpty(holdingsRecord.getCallNumber()) ? EMPTY : holdingsRecord.getCallNumber())
      .callNumberSuffix(isEmpty(holdingsRecord.getCallNumberSuffix()) ? EMPTY : holdingsRecord.getCallNumberSuffix())
      .numberOfItems(isEmpty(holdingsRecord.getNumberOfItems()) ? EMPTY : holdingsRecord.getNumberOfItems())
      .holdingsStatements(holdingsStatementsToString(holdingsRecord.getHoldingsStatements()))
      .holdingsStatementsForSupplements(holdingsStatementsToString(holdingsRecord.getHoldingsStatementsForSupplements()))
      .holdingsStatementsForIndexes(holdingsStatementsToString(holdingsRecord.getHoldingsStatementsForIndexes()))
      .illPolicy(holdingsReferenceService.getIllPolicyNameById(holdingsRecord.getIllPolicyId(), errorServiceArgs))
      .digitizationPolicy(isEmpty(holdingsRecord.getDigitizationPolicy()) ? EMPTY : holdingsRecord.getDigitizationPolicy())
      .retentionPolicy(isEmpty(holdingsRecord.getRetentionPolicy()) ? EMPTY : holdingsRecord.getRetentionPolicy())
      .actionNote(fetchNotesByTypeName(holdingsRecord.getNotes(), ACTION_NOTE))
      .bindingNote(fetchNotesByTypeName(holdingsRecord.getNotes(), BINDING))
      .copyNote(fetchNotesByTypeName(holdingsRecord.getNotes(), COPY_NOTE))
      .electronicBookplateNote(fetchNotesByTypeName(holdingsRecord.getNotes(), ELECTRONIC_BOOKPLATE))
      .note(fetchNotesByTypeName(holdingsRecord.getNotes(), NOTE))
      .provenanceNote(fetchNotesByTypeName(holdingsRecord.getNotes(), PROVENANCE))
      .reproductionNote(fetchNotesByTypeName(holdingsRecord.getNotes(), REPRODUCTION))
      .electronicAccess(electronicAccessService.getElectronicAccessesToString(holdingsRecord.getElectronicAccess(), errorServiceArgs))
      .acquisitionMethod(isEmpty(holdingsRecord.getAcquisitionMethod()) ? EMPTY : holdingsRecord.getAcquisitionMethod())
      .acquisitionFormat(isEmpty(holdingsRecord.getAcquisitionFormat()) ? EMPTY : holdingsRecord.getAcquisitionFormat())
      .tags(tagsToString(holdingsRecord.getTags()))
      .receiptStatus(isEmpty(holdingsRecord.getReceiptStatus()) ? EMPTY : holdingsRecord.getReceiptStatus())
      .build();
  }

  private String holdingsStatementsToString(List<HoldingsStatement> statements) {
    return isEmpty(statements) ? EMPTY : statements.stream()
      .filter(Objects::nonNull)
      .map(statement -> String.join(ARRAY_DELIMITER, escaper.escape(statement.getStatement()),
        escaper.escape(statement.getNote()), escaper.escape(statement.getStaffNote())))
      .collect(Collectors.joining(ITEM_DELIMITER));
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

  private String fetchNotesByTypeName(List<HoldingsNote> notes, String noteType) {
    if (!isEmpty(notes)) {
      var type = holdingsReferenceService.getHoldingsNoteTypes().get(noteType);
      return isNull(type) ?
        EMPTY :
        notes.stream()
          .filter(note -> type.equals(note.getHoldingsNoteTypeId()))
          .map(this::holdingsNoteToString)
          .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
    }
    return EMPTY;
  }

  private String holdingsNoteToString(HoldingsNote holdingsNote) {
    return holdingsNote.getNote() + (Boolean.TRUE.equals(holdingsNote.getStaffOnly()) ?
      SPACE + STAFF_ONLY : EMPTY);
  }

  public HoldingsRecord mapToHoldingsRecord(HoldingsFormat holdingsFormat) {
    return new HoldingsRecord();
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
