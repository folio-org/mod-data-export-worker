package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.AddressTypeClient;
import org.folio.dew.client.CustomFieldsClient;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.client.GroupClient;
import org.folio.dew.domain.dto.CustomField;
import org.folio.dew.error.BulkEditException;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.batch.bulkedit.jobs.processidentifiers.Utils.encode;
import static org.folio.dew.utils.Constants.QUERY_PATTERN_REF_ID;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserReferenceServiceCache {

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

  @Cacheable(cacheNames = "departmentNames")
  public String getDepartmentNameById(String id) {
    return isNull(id) ? EMPTY : departmentClient.getDepartmentById(id).getName();
  }

  @Cacheable(cacheNames = "patronGroupNames")
  public String getPatronGroupNameById(String id) {
    return isNull(id) ? EMPTY : groupClient.getGroupById(id).getGroup();
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
