package org.folio.dew.service.mapper;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.dew.utils.BulkEditProcessorHelper.booleanToStringNullSafe;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.EntityType;
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
      .notes(notesToString(holdingsRecord.getNotes(), errorServiceArgs))
      .electronicAccess(electronicAccessService.getElectronicAccessesToString(holdingsRecord.getElectronicAccess(), errorServiceArgs, HOLDINGS_RECORD))
      .acquisitionMethod(isEmpty(holdingsRecord.getAcquisitionMethod()) ? EMPTY : holdingsRecord.getAcquisitionMethod())
      .acquisitionFormat(isEmpty(holdingsRecord.getAcquisitionFormat()) ? EMPTY : holdingsRecord.getAcquisitionFormat())
      .tags(tagsToString(holdingsRecord.getTags()))
      .receiptStatus(isEmpty(holdingsRecord.getReceiptStatus()) ? EMPTY : holdingsRecord.getReceiptStatus())
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
    return new HoldingsRecord();
  }

}
