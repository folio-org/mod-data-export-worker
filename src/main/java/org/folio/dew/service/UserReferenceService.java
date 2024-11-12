package org.folio.dew.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.dew.utils.Constants.QUERY_PATTERN_REF_ID;
import static org.folio.dew.utils.Utils.encode;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.AddressTypeClient;
import org.folio.dew.client.CustomFieldsClient;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.client.GroupClient;
import org.folio.dew.domain.dto.CustomField;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
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

  @Cacheable(cacheNames = "addressTypeNames")
  public String getAddressTypeDescById(String id, ErrorServiceArgs args) {
    try {
      return isNull(id) ? EMPTY : addressTypeClient.getAddressTypeById(id).getAddressType();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Address type was not found by id: [%s]", id)), args.getFileName());
      return id;
    }
  }

  @Cacheable(cacheNames = "addressTypeIds")
  public String getAddressTypeIdByDesc(String desc) {
    if (isEmpty(desc)) {
      return null;
    } else {
      var response = addressTypeClient.getAddressTypeByQuery(String.format("desc==\"%s\"", desc));
      if (response.getAddressTypes().isEmpty()) {
        return desc;
      }
      return response.getAddressTypes().get(0).getId();
    }
  }

  @Cacheable(cacheNames = "departmentNames")
  public String getDepartmentNameById(String id, ErrorServiceArgs args) {
    try {
      return isNull(id) ? EMPTY : departmentClient.getDepartmentById(id).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Department was not found by id: [%s]", id)), args.getFileName());
      return id;
    }
  }

  @Cacheable(cacheNames = "departmentIds")
  public String getDepartmentIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    } else {
      var response = departmentClient.getDepartmentByQuery(String.format("name==\"%s\"", name));
      if (response.getDepartments().isEmpty()) {
        return name;
      }
      return response.getDepartments().get(0).getId();
    }
  }

  @Cacheable(cacheNames = "patronGroupNames")
  public String getPatronGroupNameById(String id, ErrorServiceArgs args) {
    try {
      return isNull(id) ? EMPTY : groupClient.getGroupById(id).getGroup();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Patron group was not found by id: [%s]", id)), args.getFileName());
      return id;
    }
  }

  @Cacheable(cacheNames = "customFields")
  public CustomField getCustomFieldByRefId(String refId) {
    var moduleId = moduleTenantService.getModUsersModuleId();
    return customFieldsClient.getCustomFieldsByQuery(moduleId, format(QUERY_PATTERN_REF_ID, encode(refId)))
    .getCustomFields().stream().filter(customField -> customField.getRefId().equals(refId))
      .findFirst()
      .orElseThrow(() -> new BulkEditException(format("Custom field with refId=%s not found", refId)));
  }
}
