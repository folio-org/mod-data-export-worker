package org.folio.dew.batch.bulkedit.jobs;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.folio.dew.domain.dto.JobParameterNames.AT_LEAST_ONE_MARC_EXISTS;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.DUPLICATE_ENTRY;
import static org.folio.dew.utils.Constants.LINKED_DATA_SOURCE;
import static org.folio.dew.utils.Constants.LINKED_DATA_SOURCE_IS_NOT_SUPPORTED;
import static org.folio.dew.utils.Constants.MULTIPLE_MATCHES_MESSAGE;
import static org.folio.dew.utils.Constants.MULTIPLE_SRS;
import static org.folio.dew.utils.Constants.NO_INSTANCE_VIEW_PERMISSIONS;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.dew.utils.Constants.SRS_MISSING;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionsValidator;
import org.folio.dew.client.InventoryInstancesClient;
import org.folio.dew.client.SrsClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.Instance;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.mapper.InstanceMapper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditInstanceProcessor implements ItemProcessor<ItemIdentifier, List<InstanceFormat>> {
  private final InventoryInstancesClient inventoryInstancesClient;
  private final InstanceMapper instanceMapper;
  private final FolioExecutionContext folioExecutionContext;
  private final PermissionsValidator permissionsValidator;
  private final UserClient userClient;
  private final SrsClient srsClient;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;
  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;

  private final Set<ItemIdentifier> identifiersToCheckDuplication = ConcurrentHashMap.newKeySet();
  private final Set<String> fetchedInstanceIds = ConcurrentHashMap.newKeySet();

  @Override
  public List<InstanceFormat> process(ItemIdentifier itemIdentifier) throws BulkEditException {
    log.debug("Instance processor current thread: {}", Thread.currentThread().getName());
    try {
      if (!permissionsValidator.isBulkEditReadPermissionExists(folioExecutionContext.getTenantId(), EntityType.INSTANCE)) {
        var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
        throw new BulkEditException(format(NO_INSTANCE_VIEW_PERMISSIONS, user.getUsername(), resolveIdentifier(identifierType), itemIdentifier.getItemId(), folioExecutionContext.getTenantId()), ErrorType.ERROR);
      }
      if (!identifiersToCheckDuplication.add(itemIdentifier)) {
        throw new BulkEditException(DUPLICATE_ENTRY, ErrorType.WARNING);
      }

      var instance = getInstance(itemIdentifier);

      if (LINKED_DATA_SOURCE.equals(instance.getSource())) {
        throw new BulkEditException(LINKED_DATA_SOURCE_IS_NOT_SUPPORTED, ErrorType.ERROR);
      }

      if (fetchedInstanceIds.add(instance.getId())) {
        var instanceFormat = instanceMapper.mapToInstanceFormat(instance, itemIdentifier.getItemId(), jobId, FilenameUtils.getName(fileName)).withOriginal(instance)
          .withTenantId(folioExecutionContext.getTenantId());
        checkMissingSrsAndDuplication(instanceFormat);
        return List.of(instanceFormat);
      }
      return emptyList();
    } catch (BulkEditException e) {
      throw e;
    } catch (Exception e) {
      throw new BulkEditException(e.getMessage(), ErrorType.ERROR);
    }
  }

  /**
   * Retrieves instance based on the instance identifier value. Currently, only ID and HRID are supported for instances.
   * ISBN and ISSN are not supported because they are not unique identifiers for instances.
   *
   * @param itemIdentifier the item identifier to use for retrieving instances
   * @return the instance
   * @throws BulkEditException if the identifier type is not supported
   */
  private Instance getInstance(ItemIdentifier itemIdentifier) {
    return switch (IdentifierType.fromValue(identifierType)) {
      case ID, HRID -> {
        var instances = inventoryInstancesClient.getInstanceByQuery(String.format(getMatchPattern(identifierType), resolveIdentifier(identifierType), itemIdentifier.getItemId()), 1);
        if (instances.getTotalRecords() > 1) {
          log.error(MULTIPLE_MATCHES_MESSAGE);
          throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE, ErrorType.ERROR);
        } else if (instances.getTotalRecords() < 1 || instances.getInstances().isEmpty()) {
          log.error(NO_MATCH_FOUND_MESSAGE);
          throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
        }
          yield instances.getInstances().get(0);
      }
      default -> throw new BulkEditException(String.format("Identifier type \"%s\" is not supported", identifierType), ErrorType.ERROR);
    };
  }

  private void checkMissingSrsAndDuplication(InstanceFormat instanceFormat) {
    if (instanceFormat.getSource().equals("MARC")) {
      if (!jobExecution.getExecutionContext().containsKey(AT_LEAST_ONE_MARC_EXISTS)) {
        jobExecution.getExecutionContext().put(AT_LEAST_ONE_MARC_EXISTS, true);
      }
      var srsRecords = srsClient.getMarc(instanceFormat.getId(), "INSTANCE", true).get("sourceRecords");
      if (srsRecords.isEmpty()) {
        log.error(SRS_MISSING);
        throw new BulkEditException(SRS_MISSING);
      }
      if (srsRecords.size() > 1) {
        var errorMsg = MULTIPLE_SRS.formatted(getAllSrsIds(srsRecords));
        log.error(errorMsg);
        throw new BulkEditException(errorMsg);
      }
    }
  }

  private String getAllSrsIds(JsonNode srsRecords) {
    return String.join(", ", StreamSupport.stream(srsRecords.spliterator(), false)
      .map(n -> StringUtils.strip(n.get("recordId").toString(), "\"")).toList());
  }
}
