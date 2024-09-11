package org.folio.dew.batch.bulkedit.jobs;

import static java.lang.String.format;
import static org.folio.dew.domain.dto.IdentifierType.ISBN;
import static org.folio.dew.domain.dto.IdentifierType.ISSN;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.NO_INSTANCE_VIEW_PERMISSIONS;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionsValidator;
import org.folio.dew.client.InventoryInstancesClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.Instance;
import org.folio.dew.domain.dto.InstanceCollection;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.InstanceReferenceService;
import org.folio.dew.service.mapper.InstanceMapper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditInstanceProcessor implements ItemProcessor<ItemIdentifier, List<InstanceFormat>> {
  private final InventoryInstancesClient inventoryInstancesClient;
  private final InstanceMapper instanceMapper;
  private final InstanceReferenceService instanceReferenceService;
  private final FolioExecutionContext folioExecutionContext;
  private final PermissionsValidator permissionsValidator;
  private final UserClient userClient;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;

  private Set<ItemIdentifier> identifiersToCheckDuplication = new HashSet<>();
  private Set<String> fetchedInstanceIds = new HashSet<>();

  @Override
  public List<InstanceFormat> process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (!permissionsValidator.isBulkEditReadPermissionExists(folioExecutionContext.getTenantId(), EntityType.INSTANCE)) {
      var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
      throw new BulkEditException(format(NO_INSTANCE_VIEW_PERMISSIONS, user.getUsername(), resolveIdentifier(identifierType), itemIdentifier.getItemId(), folioExecutionContext.getTenantId()));
    }
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry");
    }
    identifiersToCheckDuplication.add(itemIdentifier);

    var instances = getInstances(itemIdentifier);
    if (instances.getInstances().isEmpty()) {
      log.error(NO_MATCH_FOUND_MESSAGE);
      throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
    }

    var distinctInstances = instances.getInstances().stream()
      .filter(instance -> !fetchedInstanceIds.contains(instance.getId()))
      .toList();
    fetchedInstanceIds.addAll(distinctInstances.stream().map(Instance::getId).toList());

    var isbn = ISBN.equals(IdentifierType.fromValue(identifierType)) ? itemIdentifier.getItemId() : null;
    var issn = ISSN.equals(IdentifierType.fromValue(identifierType)) ? itemIdentifier.getItemId() : null;

    var tenantId = folioExecutionContext.getTenantId();

    return distinctInstances.stream()
      .map(r -> instanceMapper.mapToInstanceFormat(r, itemIdentifier.getItemId(), jobId, FilenameUtils.getName(fileName)).withOriginal(r))
      .map(instanceFormat -> instanceFormat.withIsbn(isbn))
      .map(instanceFormat -> instanceFormat.withIssn(issn))
      .map(instanceFormat -> instanceFormat.withTenantId(tenantId))
      .toList();
  }

  private InstanceCollection getInstances(ItemIdentifier itemIdentifier) {
    return switch (IdentifierType.fromValue(identifierType)) {
      case ID, HRID ->
      inventoryInstancesClient.getInstanceByQuery(String.format(getMatchPattern(identifierType), resolveIdentifier(identifierType), itemIdentifier.getItemId()), 1);
      case ISBN -> getInstancesByIdentifierTypeAndValue(ISBN, itemIdentifier.getItemId());
      case ISSN -> getInstancesByIdentifierTypeAndValue(ISSN, itemIdentifier.getItemId());
      default -> throw new BulkEditException(String.format("Identifier type \"%s\" is not supported", identifierType));
    };
  }

  private InstanceCollection getInstancesByIdentifierTypeAndValue(IdentifierType identifierType, String value) {
    return inventoryInstancesClient.getInstanceByQuery(String.format("(identifiers=/@identifierTypeId=%s \"%s\")",
      instanceReferenceService.getTypeOfIdentifiersIdByName(identifierType.getValue()), value), Integer.MAX_VALUE);
  }
}
