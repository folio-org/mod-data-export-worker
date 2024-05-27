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
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_SPACED;
import static org.folio.dew.utils.Constants.NOTE;
import static org.folio.dew.utils.Constants.PROVENANCE;
import static org.folio.dew.utils.Constants.REPRODUCTION;
import static org.folio.dew.utils.Constants.STAFF_ONLY;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsNote;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.folio.dew.domain.dto.HoldingsStatement;
import org.folio.dew.domain.dto.Tags;
import org.folio.dew.service.ElectronicAccessService;
import org.folio.dew.service.HoldingsReferenceService;
import org.folio.dew.service.SpecialCharacterEscaper;
import org.springframework.stereotype.Service;

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

}
