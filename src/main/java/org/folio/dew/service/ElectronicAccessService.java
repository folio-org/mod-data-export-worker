package org.folio.dew.service;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_PATTERN;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.ElectronicAccessRelationshipClient;
import org.folio.dew.domain.dto.ElectronicAccess;
import org.folio.dew.domain.dto.ElectronicAccessRelationship;
import org.folio.dew.error.BulkEditException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessService {
  private final ElectronicAccessRelationshipClient relationshipClient;

  private static final int NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS = 5;
  private static final int ELECTRONIC_ACCESS_URI_INDEX = 0;
  private static final int ELECTRONIC_ACCESS_LINK_TEXT_INDEX = 1;
  private static final int ELECTRONIC_ACCESS_MATERIAL_SPECIFICATION_INDEX = 2;
  private static final int ELECTRONIC_ACCESS_PUBLIC_NOTE_INDEX = 3;
  private static final int ELECTRONIC_ACCESS_RELATIONSHIP_INDEX = 4;

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

  @Cacheable(cacheNames = "relationships")
  public ElectronicAccessRelationship getElectronicAccessRelationshipByName(String name) {
    var relationships = relationshipClient.getByQuery("name==" + name);
    if (relationships.getElectronicAccessRelationships().isEmpty()) {
      throw new BulkEditException("Electronic access relationship not found: " + name);
    }
    return relationships.getElectronicAccessRelationships().get(0);
  }

  public List<ElectronicAccess> restoreElectronicAccess(String s) {
    return StringUtils.isEmpty(s) ? Collections.emptyList() :
      Arrays.stream(s.split(ITEM_DELIMITER_PATTERN))
        .map(this::restoreElectronicAccessItem)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private ElectronicAccess restoreElectronicAccessItem(String s) {
    if (isNotEmpty(s)) {
      var tokens = s.split(ARRAY_DELIMITER, -1);
      if (NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS == tokens.length) {
        return new ElectronicAccess()
          .uri(tokens[ELECTRONIC_ACCESS_URI_INDEX])
          .linkText(tokens[ELECTRONIC_ACCESS_LINK_TEXT_INDEX])
          .materialsSpecification(tokens[ELECTRONIC_ACCESS_MATERIAL_SPECIFICATION_INDEX])
          .publicNote(tokens[ELECTRONIC_ACCESS_PUBLIC_NOTE_INDEX])
          .relationshipId(getElectronicAccessRelationshipByName(tokens[ELECTRONIC_ACCESS_RELATIONSHIP_INDEX]).getId());
      }
      throw new BulkEditException(String.format("Illegal number of electronic access elements: %d, expected: %d", tokens.length, NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS));
    }
    return null;
  }
}
