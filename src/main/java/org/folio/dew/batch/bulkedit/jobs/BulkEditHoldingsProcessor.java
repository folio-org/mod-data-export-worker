package org.folio.dew.batch.bulkedit.jobs;

import static java.lang.String.format;
import static org.folio.dew.domain.dto.BatchIdsDto.IdentifierTypeEnum.INSTANCEHRID;
import static org.folio.dew.domain.dto.IdentifierType.HRID;
import static org.folio.dew.domain.dto.IdentifierType.ID;
import static org.folio.dew.domain.dto.IdentifierType.INSTANCE_HRID;
import static org.folio.dew.domain.dto.IdentifierType.ITEM_BARCODE;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.getResponseAsString;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.DUPLICATES_ACROSS_TENANTS;
import static org.folio.dew.utils.Constants.MULTIPLE_MATCHES_MESSAGE;
import static org.folio.dew.utils.Constants.NO_HOLDING_VIEW_PERMISSIONS;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.dew.utils.SearchIdentifierTypeResolver.getSearchIdentifierType;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionsValidator;
import org.folio.dew.batch.bulkedit.jobs.processidentifiers.DuplicationChecker;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.client.SearchClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.BatchIdsDto;
import org.folio.dew.domain.dto.ConsortiumHolding;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.domain.dto.ExtendedHoldingsRecord;
import org.folio.dew.domain.dto.ExtendedHoldingsRecordCollection;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsRecordCollection;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.ConsortiaService;
import org.folio.dew.service.FolioExecutionContextManager;
import org.folio.dew.service.HoldingsReferenceService;
import org.folio.dew.service.mapper.HoldingsMapper;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditHoldingsProcessor extends FolioExecutionContextManager implements ItemProcessor<ItemIdentifier, List<HoldingsFormat>> {
  private final HoldingClient holdingClient;
  private final HoldingsMapper holdingsMapper;
  private final HoldingsReferenceService holdingsReferenceService;
  private final SearchClient searchClient;
  private final ConsortiaService consortiaService;
  private final FolioExecutionContext folioExecutionContext;
  private final UserClient userClient;
  private final PermissionsValidator permissionsValidator;
  private final TenantResolver tenantResolver;
  private final DuplicationChecker duplicationChecker;

  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;
  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;

  @Override
  public List<HoldingsFormat> process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (duplicationChecker.isDuplicate(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry", ErrorType.WARNING);
    }

    var holdings = getHoldingsRecords(itemIdentifier);
    var distinctHoldings = holdings.getExtendedHoldingsRecords().stream()
      .filter(holdingsRecord -> duplicationChecker.wasNotFetched(holdingsRecord.getEntity().getId()))
      .toList();
    duplicationChecker.addAll(distinctHoldings.stream().map(extendedHoldingsRecord -> extendedHoldingsRecord.getEntity().getId()).toList());

    var instanceHrid = INSTANCE_HRID == IdentifierType.fromValue(identifierType) ? itemIdentifier.getItemId() : null;
    var itemBarcode = ITEM_BARCODE == IdentifierType.fromValue(identifierType) ? itemIdentifier.getItemId() : null;

    return distinctHoldings.stream()
      .map(extendedHoldingsRecord -> holdingsMapper.mapToHoldingsFormat(extendedHoldingsRecord, itemIdentifier.getItemId(), jobId, FilenameUtils.getName(fileName)).withOriginal(extendedHoldingsRecord.getEntity()))
      .map(holdingsFormat -> holdingsFormat.withInstanceHrid(instanceHrid))
      .map(holdingsFormat -> holdingsFormat.withItemBarcode(itemBarcode))
      .toList();
  }

  private ExtendedHoldingsRecordCollection getHoldingsRecords(ItemIdentifier itemIdentifier) {
    var type = IdentifierType.fromValue(identifierType);
    var identifier = itemIdentifier.getItemId();

    var centralTenantId = consortiaService.getCentralTenantId();
    if (isCurrentTenantCentral(centralTenantId)) {
      // Process central tenant
      var identifierTypeEnum = getSearchIdentifierType(type);
      var consortiumHoldingsCollection = searchClient.getConsortiumHoldingCollection(new BatchIdsDto()
          .identifierType(getSearchIdentifierType(type))
        .identifierValues(List.of(identifier)));
      if (consortiumHoldingsCollection.getTotalRecords() > 0) {
        var extendedHoldingsRecordCollection = new ExtendedHoldingsRecordCollection()
          .extendedHoldingsRecords(new ArrayList<>())
          .totalRecords(0);
        var tenantIds = consortiumHoldingsCollection.getHoldings()
          .stream()
          .map(ConsortiumHolding::getTenantId).collect(Collectors.toSet());
        if (INSTANCEHRID != identifierTypeEnum && tenantIds.size() > 1) {
          throw new BulkEditException(DUPLICATES_ACROSS_TENANTS, ErrorType.ERROR);
        }
        var affiliatedPermittedTenants = tenantResolver.getAffiliatedPermittedTenantIds(EntityType.HOLDINGS_RECORD,
          jobExecution, identifierType, tenantIds, itemIdentifier);
        affiliatedPermittedTenants.forEach(tenantId -> {
          try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
            var holdingsRecordCollection = getHoldingsRecordCollection(type, itemIdentifier);
            extendedHoldingsRecordCollection.getExtendedHoldingsRecords().addAll(
              holdingsRecordCollection.getHoldingsRecords().stream()
                .map(holdingsRecord -> new ExtendedHoldingsRecord().tenantId(tenantId).entity(holdingsRecord)).toList()
            );
            extendedHoldingsRecordCollection.setTotalRecords(extendedHoldingsRecordCollection.getTotalRecords() + holdingsRecordCollection.getTotalRecords());
          } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
          }
        });
        return extendedHoldingsRecordCollection;
      } else {
        throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
      }
    } else {
      // Process local tenant case
      checkReadPermissions(folioExecutionContext.getTenantId(), identifier);
      var holdingsRecordCollection = getHoldingsRecordCollection(type, itemIdentifier);
      var extendedHoldingsRecordCollection =  new ExtendedHoldingsRecordCollection().extendedHoldingsRecords(holdingsRecordCollection.getHoldingsRecords().stream()
          .map(holdingsRecord -> new ExtendedHoldingsRecord().tenantId(folioExecutionContext.getTenantId()).entity(holdingsRecord)).toList())
        .totalRecords(holdingsRecordCollection.getTotalRecords());
      if (extendedHoldingsRecordCollection.getExtendedHoldingsRecords().isEmpty()) {
        throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
      }
      return extendedHoldingsRecordCollection;
    }
  }

  private void checkReadPermissions(String tenantId, String identifier) {
    if (!permissionsValidator.isBulkEditReadPermissionExists(tenantId, EntityType.HOLDINGS_RECORD)) {
      var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
      throw new BulkEditException(format(NO_HOLDING_VIEW_PERMISSIONS, user.getUsername(), resolveIdentifier(identifierType), identifier, tenantId), ErrorType.ERROR);
    }
  }

  private boolean isCurrentTenantCentral(String centralTenantId) {
    return StringUtils.isNotEmpty(centralTenantId) && centralTenantId.equals(folioExecutionContext.getTenantId());
  }

  private HoldingsRecordCollection getHoldingsRecordCollection(IdentifierType type,  ItemIdentifier itemIdentifier) {
    if (ID == type || HRID == type) {
      var url = format(getMatchPattern(identifierType), resolveIdentifier(identifierType), itemIdentifier.getItemId());
      var holdingsRecordCollection = holdingClient.getHoldingsByQuery(url);
      if (holdingsRecordCollection.getTotalRecords() > 1) {
        log.error("Response from {} for tenant {}: {}", url, folioExecutionContext.getTenantId(), getResponseAsString(holdingsRecordCollection));
        throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE, ErrorType.ERROR);
      }
      return holdingsRecordCollection;
    } else if (INSTANCE_HRID == type) {
      return holdingClient.getHoldingsByQuery("instanceId==" + holdingsReferenceService.getInstanceIdByHrid(itemIdentifier.getItemId()), Integer.MAX_VALUE);
    } else if (ITEM_BARCODE == type) {
      return holdingClient.getHoldingsByQuery("id==" + holdingsReferenceService.getHoldingsIdByItemBarcode(itemIdentifier.getItemId()), 1);
    } else {
      throw new BulkEditException(format("Identifier type \"%s\" is not supported", identifierType), ErrorType.ERROR);
    }
  }
}
