package org.folio.dew.service;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.Constants.ENTITY_TYPE_TO_ELECTRONIC_ACCESS_DATA_DELIMITER;
import static org.folio.dew.utils.Constants.ENTITY_TYPE_TO_ELECTRONIC_ACCESS_DELIMITER;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ElectronicAccess;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessService extends FolioExecutionContextManager {
  private final BulkEditProcessingErrorsService errorsService;
  private final ElectronicAccessServiceCache electronicAccessServiceCache;

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

  public String getRelationshipNameById(String id, ErrorServiceArgs errorServiceArgs, String tenantId) {
    try {
      return electronicAccessServiceCache.getRelationshipNameById(id, tenantId);
    } catch (NotFoundException e) {
      var errorMessage = String.format("Electronic access relationship not found by id=%s", id);
      log.error(errorMessage);
      errorsService.saveErrorInCSV(errorServiceArgs.getJobId(), errorServiceArgs.getIdentifier(), new BulkEditException(errorMessage, ErrorType.WARNING), errorServiceArgs.getFileName());
      return id;
    }
  }
}
