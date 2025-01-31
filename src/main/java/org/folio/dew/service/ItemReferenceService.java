package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.springframework.stereotype.Service;


@Service
@Log4j2
@RequiredArgsConstructor
public class ItemReferenceService extends FolioExecutionContextManager {

  private final BulkEditProcessingErrorsService errorsService;
  private final ItemReferenceServiceCache itemReferenceServiceCache;


  public String getCallNumberTypeNameById(String callNumberTypeId, ErrorServiceArgs args, String tenantId) {
    try {
      return itemReferenceServiceCache.getCallNumberTypeNameById(callNumberTypeId, tenantId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Call number type was not found by id: [%s]", callNumberTypeId), ErrorType.WARNING), args.getFileName());
      return callNumberTypeId;
    }
  }

  public String getDamagedStatusNameById(String damagedStatusId, ErrorServiceArgs args, String tenantId) {
    try {
      return itemReferenceServiceCache.getDamagedStatusNameById(damagedStatusId, tenantId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Damaged status was not found by id: [%s]", damagedStatusId), ErrorType.WARNING), args.getFileName());
      return damagedStatusId;
    }
  }

  public String getNoteTypeNameById(String noteTypeId, ErrorServiceArgs args, String tenantId) {
    try {
      return itemReferenceServiceCache.getNoteTypeNameById(noteTypeId, tenantId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Note type was not found by id: [%s]", noteTypeId), ErrorType.WARNING), args.getFileName());
      return noteTypeId;
    }
  }

  public String getStatisticalCodeById(String statisticalCodeId, ErrorServiceArgs args, String tenantId) {
    try {
      return itemReferenceServiceCache.getStatisticalCodeById(statisticalCodeId, tenantId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Statistical code was not found by id: [%s]", statisticalCodeId), ErrorType.WARNING), args.getFileName());
      return statisticalCodeId;
    }
  }
}
