package org.folio.dew.service;

import static java.lang.String.format;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.AddressTypeClient;
import org.folio.dew.client.CustomFieldsClient;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.client.GroupClient;
import org.folio.dew.client.OkapiClient;
import org.folio.dew.client.ProxiesForClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.AddressType;
import org.folio.dew.domain.dto.AddressTypeCollection;
import org.folio.dew.domain.dto.CustomField;
import org.folio.dew.domain.dto.Department;
import org.folio.dew.domain.dto.DepartmentCollection;
import org.folio.dew.domain.dto.ProxyFor;
import org.folio.dew.domain.dto.ProxyForCollection;
import org.folio.dew.domain.dto.UserCollection;
import org.folio.dew.domain.dto.UserGroup;
import org.folio.dew.domain.dto.UserGroupCollection;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserReferenceService {
  private static final String OKAPI_URL = "http://_";
  private static final String MOD_USERS = "mod-users";

  private final AddressTypeClient addressTypeClient;
  private final DepartmentClient departmentClient;
  private final GroupClient groupClient;
  private final ProxiesForClient proxiesForClient;
  private final UserClient userClient;
  private final CustomFieldsClient customFieldsClient;
  private final FolioExecutionContext folioExecutionContext;
  private final OkapiClient okapiClient;

  @Cacheable(cacheNames = "addressTypes")
  public AddressType getAddressTypeById(String id) {
    return addressTypeClient.getAddressTypeById(id);
  }

  @Cacheable(cacheNames = "addressTypes")
  public AddressTypeCollection getAddressTypeByDesc(String name) {
    return addressTypeClient.getAddressTypeByQuery("desc=" + name);
  }

  @Cacheable(cacheNames = "departments")
  public Department getDepartmentById(String id) {
    return departmentClient.getDepartmentById(id);
  }

  @Cacheable(cacheNames = "departments")
  public DepartmentCollection getDepartmentByName(String name) {
    return departmentClient.getDepartmentByQuery("name=" + name);
  }

  @Cacheable(cacheNames = "userGroups")
  public UserGroup getUserGroupById(String id) {
    return groupClient.getGroupById(id);
  }

  @Cacheable(cacheNames = "userGroups")
  public UserGroupCollection getUserGroupByGroupName(String name) {
    return groupClient.getGroupByQuery(String.format("group==\"%s\"", name));
  }

  @Cacheable(cacheNames = "proxies")
  public ProxyFor getProxyForById(String id) {
    return proxiesForClient.getProxiesForById(id);
  }

  @Cacheable(cacheNames = "proxies")
  public ProxyForCollection getProxyForByProxyUserId(String id) {
    return proxiesForClient.getProxiesForByQuery("proxyUserId=" + id);
  }

  @Cacheable(cacheNames = "users")
  public UserCollection getUserByName(String name) {
    return userClient.getUserByQuery("username=" + name);
  }

  @Cacheable(cacheNames = "customFields")
  public CustomField getCustomFieldByRefId(String refId) {

    var customFields = customFieldsClient.getCustomFieldsByQuery(getModuleId(MOD_USERS),"refId==" + refId);
    if (customFields.getCustomFields().isEmpty()) {
      var msg = format("Custom field with refId=%s not found", refId);
      log.error(msg);
      throw new BulkEditException(msg);
    }
    return customFields.getCustomFields().get(0);
  }

  @Cacheable(cacheNames = "customFields")
  public CustomField getCustomFieldByName(String name) {
    var customFields = customFieldsClient.getCustomFieldsByQuery(getModuleId(MOD_USERS), "name==" + name);
    if (customFields.getCustomFields().isEmpty()) {
      var msg = format("Custom field with name=%s not found", name);
      log.error(msg);
      throw new BulkEditException(msg);
    }
    return customFields.getCustomFields().get(0);
  }

  @Cacheable(cacheNames = "moduleIds")
  public String getModuleId(String moduleName) {
    var tenantId = folioExecutionContext.getTenantId();
    var moduleNamesJson = okapiClient.getModuleIds(URI.create(OKAPI_URL), tenantId, moduleName);
    if (!moduleNamesJson.isEmpty()) {
      return moduleNamesJson.get(0).get("id").asText();
    }
    var msg = "Module id not found for name: " + moduleName;
    log.error(msg);
    throw new NotFoundException(msg);
  }
}
