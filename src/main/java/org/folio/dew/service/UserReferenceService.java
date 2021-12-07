package org.folio.dew.service;

import org.folio.dew.client.AddressTypeClient;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.client.GroupClient;
import org.folio.dew.client.ProxiesForClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.AddressType;
import org.folio.dew.domain.dto.Department;
import org.folio.dew.domain.dto.ProxyFor;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserGroup;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserReferenceService {

  private static final String QUERY_PARAM = "query=";

  private final AddressTypeClient addressTypeClient;
  private final DepartmentClient departmentClient;
  private final GroupClient groupClient;
  private final ProxiesForClient proxiesForClient;
  private final UserClient userClient;

  @Cacheable(cacheNames = "addressTypes")
  public AddressType getAddressTypeById(String id) {
    return addressTypeClient.getAddressTypeById(id);
  }

  @Cacheable(cacheNames = "addressTypes")
  public AddressType getAddressTypeByName(String name) {
    return addressTypeClient.getAddressTypeByQuery(QUERY_PARAM + "addressType=" + name);
  }

  @Cacheable(cacheNames = "departments")
  public Department getDepartmentById(String id) {
    return departmentClient.getDepartmentById(id);
  }

  @Cacheable(cacheNames = "departments")
  public Department getDepartmentByName(String name) {
    return departmentClient.getDepartmentByQuery(QUERY_PARAM + "name=" + name);
  }

  @Cacheable(cacheNames = "userGroups")
  public UserGroup getUserGroupById(String id) {
    return groupClient.getGroupById(id);
  }

  @Cacheable(cacheNames = "userGroups")
  public UserGroup getUserGroupByGroupName(String name) {
    return groupClient.getGroupByQuery(QUERY_PARAM + "group=" + name);
  }

  @Cacheable(cacheNames = "proxies")
  public ProxyFor getProxyForById(String id) {
    return proxiesForClient.getProxiesForById(id);
  }

  @Cacheable(cacheNames = "users")
  public User getUserByName(String name) {
    return userClient.getUserByQuery(QUERY_PARAM + "username=" + name);
  }

}
