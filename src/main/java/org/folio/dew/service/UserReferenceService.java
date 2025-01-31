package org.folio.dew.service;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.CustomField;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserReferenceService {

  private final BulkEditProcessingErrorsService errorsService;
  private final UserReferenceServiceCache userReferenceServiceCache;

  public String getAddressTypeDescById(String id, ErrorServiceArgs args) {
    try {
      return userReferenceServiceCache.getAddressTypeDescById(id);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Address type was not found by id: [%s]", id), ErrorType.WARNING), args.getFileName());
      return id;
    }
  }

  public String getDepartmentNameById(String id, ErrorServiceArgs args) {
    try {
      return userReferenceServiceCache.getDepartmentNameById(id);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Department was not found by id: [%s]", id), ErrorType.WARNING), args.getFileName());
      return id;
    }
  }

  public String getPatronGroupNameById(String id, ErrorServiceArgs args) {
    try {
      return userReferenceServiceCache.getPatronGroupNameById(id);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Patron group was not found by id: [%s]", id), ErrorType.WARNING), args.getFileName());
      return id;
    }
  }

  public CustomField getCustomFieldByRefId(String refId) {
    return userReferenceServiceCache.getCustomFieldByRefId(refId);
  }
}
