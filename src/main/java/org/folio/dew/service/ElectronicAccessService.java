package org.folio.dew.service;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_PATTERN;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.ElectronicAccessRelationshipClient;
import org.folio.dew.domain.dto.ElectronicAccess;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessService {
  private final ElectronicAccessRelationshipClient relationshipClient;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;

  private static final int NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS = 6;
  private static final int ELECTRONIC_ACCESS_URI_INDEX = 0;
  private static final int ELECTRONIC_ACCESS_LINK_TEXT_INDEX = 1;
  private static final int ELECTRONIC_ACCESS_MATERIAL_SPECIFICATION_INDEX = 2;
  private static final int ELECTRONIC_ACCESS_PUBLIC_NOTE_INDEX = 3;

  public static final String HOLDINGS_DELIMETER = "\u001f|";

  public String getElectronicAccessesToString(List<ElectronicAccess> electronicAccesses, String formatIdentifier, String jobId, String fileName) {
    var errors = new HashSet<String>();
    var stringOutput = isEmpty(electronicAccesses) ?
      EMPTY :
      "URL relationship;URI;Link text;Materials specified;URL public note\n" +
      electronicAccesses.stream()
        .filter(Objects::nonNull)
        .map(electronicAccess -> this.electronicAccessToString(electronicAccess, errors))
        .collect(Collectors.joining(HOLDINGS_DELIMETER));
    errors.forEach(e -> bulkEditProcessingErrorsService.saveErrorInCSV(jobId, formatIdentifier, new BulkEditException(e), fileName));

    return stringOutput;
  }

  private String electronicAccessToString(ElectronicAccess access, Set<String> errors) {
    var relationshipNameAndId = isEmpty(access.getRelationshipId()) ? ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER : getRelationshipNameById(access.getRelationshipId());
    if (isNotEmpty(access.getRelationshipId()) && relationshipNameAndId.startsWith(ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER))
      errors.add("Electronic access relationship not found by id=" + access.getRelationshipId());
    return String.join(ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER,
      access.getUri(),
      isEmpty(access.getLinkText()) ? EMPTY : access.getLinkText(),
      isEmpty(access.getMaterialsSpecification()) ? EMPTY : access.getMaterialsSpecification(),
      isEmpty(access.getPublicNote()) ? EMPTY : access.getPublicNote(),
      relationshipNameAndId);
  }

  @Cacheable(cacheNames = "relationships")
  public String getRelationshipNameById(String id) {
    try {
      return relationshipClient.getById(id).getName();
    } catch (NotFoundException e) {
      return ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER + id;
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
          .relationshipId(tokens[tokens.length - 1]);
      }
      throw new BulkEditException(String.format("Illegal number of electronic access elements: %d, expected: %d", tokens.length, NUMBER_OF_ELECTRONIC_ACCESS_COMPONENTS));
    }
    return null;
  }
}
