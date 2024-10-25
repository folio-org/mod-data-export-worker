package org.folio.dew.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import org.folio.dew.client.ConsortiumClient;
import org.folio.dew.domain.bean.Consortia;
import org.folio.dew.domain.bean.ConsortiaCollection;
import org.folio.dew.domain.dto.UserTenant;
import org.folio.dew.domain.dto.UserTenantCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ConsortiaServiceTest {

  @Mock
  private ConsortiumClient consortiumClient;

  @InjectMocks
  private ConsortiaService consortiaService;

  @Test
  void testGetAffiliatedTenants() {
    var userId = "userId";

    var consortia = new Consortia("consortia");
    var consortiaCollection = new ConsortiaCollection();
    consortiaCollection.setConsortia(List.of(consortia));

    var userTenant = new UserTenant();
    userTenant.setTenantId("member");
    var userTenantCollection = new UserTenantCollection();
    userTenantCollection.setUserTenants(List.of(userTenant));

    when(consortiumClient.getConsortia()).thenReturn(consortiaCollection);
    when(consortiumClient.getConsortiaUserTenants(consortia.getId(), userId, Integer.MAX_VALUE)).thenReturn(userTenantCollection);

    var affiliatedTenants = consortiaService.getAffiliatedTenants("currentTenantId", userId);

    assertFalse(affiliatedTenants.isEmpty());
    assertEquals("member", affiliatedTenants.get(0));
  }
}
