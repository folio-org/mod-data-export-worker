package org.folio.dew.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.dew.batch.bulkedit.jobs.processidentifiers.Utils.encode;
import static org.folio.dew.utils.Constants.QUERY_PATTERN_REF_ID;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.AddressTypeClient;
import org.folio.dew.client.CustomFieldsClient;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.client.GroupClient;
import org.folio.dew.domain.dto.CustomField;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserReferenceService {

  private final AddressTypeClient addressTypeClient;
  private final DepartmentClient departmentClient;
  private final GroupClient groupClient;
  private final CustomFieldsClient customFieldsClient;
  private final BulkEditProcessingErrorsService errorsService;
  private final ModuleTenantService moduleTenantService;
  private final FolioExecutionContext folioExecutionContext;

  @Cacheable(cacheNames = "addressTypeNames")
  public String getAddressTypeDescById(String id) {
    return isNull(id) ? EMPTY : addressTypeClient.getAddressTypeById(id).getAddressType();
  }


  public String getAddressTypeDescById(String id, ErrorServiceArgs args) {
    try {
      return getAddressTypeDescById(id);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Address type was not found by id: [%s]", id)), args.getFileName());
      return id;
    }
  }

  @Cacheable(cacheNames = "departmentNames")
  public String getDepartmentNameById(String id) {
    return isNull(id) ? EMPTY : departmentClient.getDepartmentById(id).getName();
  }

  public String getDepartmentNameById(String id, ErrorServiceArgs args) {
    try {
      return getDepartmentNameById(id);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Department was not found by id: [%s]", id)), args.getFileName());
      return id;
    }
  }

  @Cacheable(cacheNames = "patronGroupNames")
  public String getPatronGroupNameById(String id) {
    return isNull(id) ? EMPTY : groupClient.getGroupById(id).getGroup();
  }

  public String getPatronGroupNameById(String id, ErrorServiceArgs args) {
    try {
      return getPatronGroupNameById(id);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Patron group was not found by id: [%s]", id)), args.getFileName());
      return id;
    }
  }

  @Cacheable(cacheNames = "customFields")
  public CustomField getCustomFieldByRefId(String refId) {
    var moduleId = moduleTenantService.getModUsersModuleId();
    return customFieldsClient.getCustomFieldsByQuery(moduleId, format(QUERY_PATTERN_REF_ID, encode(refId))).getCustomFields()
      .stream().filter(customField -> customField.getRefId().equals(refId))
      .findFirst()
      .orElseThrow(() -> new BulkEditException(format("Custom field with refId=%s not found", refId)));
  }
}
