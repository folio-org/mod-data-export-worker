package org.folio.dew.service;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.ElectronicAccessRelationshipClient;
import org.folio.dew.domain.dto.ElectronicAccess;
import org.folio.dew.domain.dto.ElectronicAccessRelationship;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessService {
  private final ElectronicAccessRelationshipClient relationshipClient;

  public String electronicAccessesToString(List<ElectronicAccess> electronicAccesses) {
    return isEmpty(electronicAccesses) ?
      EMPTY :
      electronicAccesses.stream()
        .map(this::electronicAccessToString)
        .collect(Collectors.joining(ITEM_DELIMITER));
  }

  private String electronicAccessToString(ElectronicAccess access) {
    var relationship = isEmpty(access.getRelationshipId()) ? null : getRelationshipById(access.getRelationshipId());
    return String.join(ARRAY_DELIMITER,
      access.getUri(),
      isEmpty(access.getLinkText()) ? EMPTY : access.getLinkText(),
      isEmpty(access.getMaterialsSpecification()) ? EMPTY : access.getMaterialsSpecification(),
      isEmpty(access.getPublicNote()) ? EMPTY : access.getPublicNote(),
      isNull(relationship) ? EMPTY : relationship.getName());
  }

  @Cacheable(cacheNames = "relationships")
  public ElectronicAccessRelationship getRelationshipById(String id) {
    return relationshipClient.getById(id);
  }
}
