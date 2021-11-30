package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import org.folio.dew.client.AddressTypeClient;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.client.GroupClient;
import org.folio.dew.client.ProxiesForClient;
import org.folio.dew.domain.dto.AddressType;
import org.folio.dew.domain.dto.Department;
import org.folio.dew.domain.dto.ProxyFor;
import org.folio.dew.domain.dto.UserGroup;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserReferenceService {
  private final AddressTypeClient addressTypeClient;
  private final DepartmentClient departmentClient;
  private final GroupClient groupClient;
  private final ProxiesForClient proxiesForClient;

  @Cacheable(cacheNames = "addressTypes")
  public AddressType getAddressTypeById(String id) {
    return addressTypeClient.getAddressTypeById(id);
  }

  @Cacheable(cacheNames = "departments")
  public Department getDepartmentById(String id) {
    return departmentClient.getDepartmentsById(id);
  }

  @Cacheable(cacheNames = "userGroups")
  public UserGroup getUserGroupById(String id) {
    return groupClient.getGroupById(id);
  }

  @Cacheable(cacheNames = "proxies")
  public ProxyFor getProxyForById(String id) {
    return proxiesForClient.getProxiesForById(id);
  }
}
