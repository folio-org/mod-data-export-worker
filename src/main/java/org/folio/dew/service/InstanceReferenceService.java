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
public class InstanceReferenceService {

  private final BulkEditProcessingErrorsService errorsService;
  private final InstanceReferenceServiceCache instanceReferenceServiceCache;

  public String getInstanceStatusNameById(String instanceStatusId, ErrorServiceArgs args) {
    try {
      return instanceReferenceServiceCache.getInstanceStatusNameById(instanceStatusId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Instance status was not found by id: [%s]", instanceStatusId), ErrorType.WARNING), args.getFileName());
      return instanceStatusId;
    }
  }

  public String getModeOfIssuanceNameById(String issuanceModeId, ErrorServiceArgs args) {
    try {
      return instanceReferenceServiceCache.getModeOfIssuanceNameById(issuanceModeId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Issuance mode was not found by id: [%s]", issuanceModeId), ErrorType.WARNING), args.getFileName());
      return issuanceModeId;
    }
  }

  public String getInstanceTypeNameById(String instanceTypeId, ErrorServiceArgs args) {
    try {
      return instanceReferenceServiceCache.getInstanceTypeNameById(instanceTypeId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Instance type was not found by id: [%s]", instanceTypeId), ErrorType.WARNING), args.getFileName());
      return instanceTypeId;
    }
  }

  public String getNatureOfContentTermNameById(String natureOfContentTermId, ErrorServiceArgs args) {
    try {
      return instanceReferenceServiceCache.getNatureOfContentTermNameById(natureOfContentTermId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Nature of content term was not found by id: [%s]", natureOfContentTermId), ErrorType.WARNING), args.getFileName());
      return natureOfContentTermId;
    }
  }

  public String getFormatOfInstanceNameById(String instanceFormatId, ErrorServiceArgs args) {
    try {
      return instanceReferenceServiceCache.getFormatOfInstanceNameById(instanceFormatId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Instance format was not found by id: [%s]", instanceFormatId), ErrorType.WARNING), args.getFileName());
      return instanceFormatId;
    }
  }

  public String getTypeOfIdentifiersIdByName(String identifierName) {
    return instanceReferenceServiceCache.getTypeOfIdentifiersIdByName(identifierName);
  }

  public String getInstanceNoteTypeNameById(String noteTypeId, ErrorServiceArgs args) {
    try {
      return instanceReferenceServiceCache.getInstanceNoteTypeNameById(noteTypeId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Instance note type was not found by id: [%s]", noteTypeId), ErrorType.WARNING), args.getFileName());
      return noteTypeId;
    }
  }

  public String getStatisticalCodeNameById(String statisticalCodeId, ErrorServiceArgs args) {
    try {
      return instanceReferenceServiceCache.getStatisticalCodeNameById(statisticalCodeId);
    } catch (NotFoundException e) {
      var msg = "Statistical code not found by id=" + statisticalCodeId;
      log.error(msg);
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(msg, ErrorType.WARNING), args.getFileName());
      return statisticalCodeId;
    }
  }

  public String getStatisticalCodeCodeById(String statisticalCodeId, ErrorServiceArgs args) {
    try {
      return instanceReferenceServiceCache.getStatisticalCodeCodeById(statisticalCodeId);
    } catch (NotFoundException e) {
      var msg = "Statistical code not found by id=" + statisticalCodeId;
      log.error(msg);
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(msg, ErrorType.WARNING), args.getFileName());
      return statisticalCodeId;
    }
  }

  public String getStatisticalCodeTypeNameById(String statisticalCodeId, ErrorServiceArgs args) {
    try {
      return instanceReferenceServiceCache.getStatisticalCodeTypeNameById(statisticalCodeId);
    } catch (NotFoundException e) {
      var msg = "Statistical code type not found by statistical code id=" + statisticalCodeId;
      log.error(msg);
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(msg, ErrorType.WARNING), args.getFileName());
      return statisticalCodeId;
    }
  }
}
