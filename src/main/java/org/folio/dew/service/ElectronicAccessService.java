package org.folio.dew.service;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ENTITY_TYPE_TO_ELECTRONIC_ACCESS_DATA_DELIMITER;
import static org.folio.dew.utils.Constants.ENTITY_TYPE_TO_ELECTRONIC_ACCESS_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_PATTERN;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.ElectronicAccessRelationshipClient;
import org.folio.dew.domain.dto.ElectronicAccess;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
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
public class ElectronicAccessService extends FolioExecutionContextManager {
  private final ElectronicAccessRelationshipClient relationshipClient;
  private final BulkEditProcessingErrorsService errorsService;
  private final FolioExecutionContext folioExecutionContext;

  private static final int NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS = 6;
  private static final int ELECTRONIC_ACCESS_URI_INDEX = 0;
  private static final int ELECTRONIC_ACCESS_LINK_TEXT_INDEX = 1;
  private static final int ELECTRONIC_ACCESS_MATERIAL_SPECIFICATION_INDEX = 2;
  private static final int ELECTRONIC_ACCESS_PUBLIC_NOTE_INDEX = 3;


  public String getElectronicAccessesToString(List<ElectronicAccess> electronicAccesses, ErrorServiceArgs errorServiceArgs, EntityType entityType, String tenantId) {
    return isEmpty(electronicAccesses) ?
      EMPTY :
      "URL relationship;URI;Link text;Materials specified;URL public note\n" +
      electronicAccesses.stream()
        .filter(Objects::nonNull)
        .map(ea -> electronicAccessToString(ea, errorServiceArgs, entityType, tenantId))
        .collect(Collectors.joining(ENTITY_TYPE_TO_ELECTRONIC_ACCESS_DELIMITER.get(entityType)));
  }

  private String electronicAccessToString(ElectronicAccess access, ErrorServiceArgs errorServiceArgs, EntityType entityType, String tenantId) {
    var relationshipName = isEmpty(access.getRelationshipId()) ? EMPTY : getRelationshipNameById(access.getRelationshipId(), errorServiceArgs,tenantId);
    return String.join(ENTITY_TYPE_TO_ELECTRONIC_ACCESS_DATA_DELIMITER.get(entityType),
      isEmpty(relationshipName) ? EMPTY : relationshipName,
      isEmpty(access.getUri()) ? EMPTY : access.getUri(),
      isEmpty(access.getLinkText()) ? EMPTY : access.getLinkText(),
      isEmpty(access.getMaterialsSpecification()) ? EMPTY : access.getMaterialsSpecification(),
      isEmpty(access.getPublicNote()) ? EMPTY : access.getPublicNote());
  }

  @Cacheable(cacheNames = "relationships")
  public String getRelationshipNameById(String id, ErrorServiceArgs errorServiceArgs, String tenantId) {
    try {
      try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
        return relationshipClient.getById(id).getName();
      }
    } catch (NotFoundException e) {
      var errorMessage = String.format("Electronic access relationship not found by id=%s", id);
      log.error(errorMessage);
      errorsService.saveErrorInCSV(errorServiceArgs.getJobId(), errorServiceArgs.getIdentifier(), new BulkEditException(errorMessage), errorServiceArgs.getFileName());
      return id;
    }
  }

  @Cacheable(cacheNames = "relationships")
  public String getElectronicAccessRelationshipIdByName(String name) {
    var relationships = relationshipClient.getByQuery(String.format("name==\"%s\"", name));
    if (relationships.getElectronicAccessRelationships().isEmpty()) {
      return EMPTY;
    }
    return relationships.getElectronicAccessRelationships().get(0).getId();
  }

  public List<ElectronicAccess> restoreElectronicAccess(String s) {
    return StringUtils.isEmpty(s) ? Collections.emptyList() :
      Arrays.stream(s.split(ITEM_DELIMITER_PATTERN))
        .map(this::restoreElectronicAccessItem)
        .filter(Objects::nonNull)
        .toList();
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
          .relationshipId(tokens[tokens.length - 1]);
      }
      throw new BulkEditException(String.format("Illegal number of electronic access elements: %d, expected: %d", tokens.length, NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS));
    }
    return null;
  }
}
