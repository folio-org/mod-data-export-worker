package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import org.folio.dew.client.CallNumberTypeClient;
import org.folio.dew.client.DamagedStatusClient;
import org.folio.dew.client.ElectronicAccessRelationshipClient;
import org.folio.dew.client.ItemNoteTypeClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.client.StatisticalCodeClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.CallNumberType;
import org.folio.dew.domain.dto.DamagedStatus;
import org.folio.dew.domain.dto.ElectronicAccessRelationship;
import org.folio.dew.domain.dto.NoteType;
import org.folio.dew.domain.dto.ServicePoint;
import org.folio.dew.domain.dto.StatisticalCode;
import org.folio.dew.domain.dto.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemReferenceService {

  private final CallNumberTypeClient callNumberTypeClient;
  private final DamagedStatusClient damagedStatusClient;
  private final ItemNoteTypeClient itemNoteTypeClient;
  private final ElectronicAccessRelationshipClient relationshipClient;
  private final ServicePointClient servicePointClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final UserClient userClient;

  @Cacheable(cacheNames = "callNumberTypes")
  public CallNumberType getCallNumberTypeById(String id) {
    return callNumberTypeClient.getById(id);
  }

  @Cacheable(cacheNames = "damagedStatuses")
  public DamagedStatus getDamagedStatusById(String id) {
    return damagedStatusClient.getById(id);
  }

  @Cacheable(cacheNames = "noteTypes")
  public NoteType getNoteTypeById(String id) {
    return itemNoteTypeClient.getById(id);
  }

  @Cacheable(cacheNames = "relationships")
  public ElectronicAccessRelationship getRelationshipById(String id) {
    return relationshipClient.getById(id);
  }

  @Cacheable(cacheNames = "servicePoints")
  public ServicePoint getServicePointById(String id) {
    return servicePointClient.getById(id);
  }

  @Cacheable(cacheNames = "statisticalCodes")
  public StatisticalCode getStatisticalCodeById(String id) {
    return statisticalCodeClient.getById(id);
  }

  @Cacheable(cacheNames = "staffMembers")
  public User getStaffMemberById(String id) {
    return userClient.getUserById(id);
  }
}
