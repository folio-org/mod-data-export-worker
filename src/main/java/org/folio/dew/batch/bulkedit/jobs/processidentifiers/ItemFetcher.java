package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static java.lang.String.format;
import static org.folio.dew.domain.dto.BatchIdsDto.IdentifierTypeEnum.HOLDINGSRECORDID;
import static org.folio.dew.domain.dto.IdentifierType.HOLDINGS_RECORD_ID;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.getResponseAsString;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.CANNOT_GET_ITEM_FROM_INVENTORY_THROUGH_QUERY;
import static org.folio.dew.utils.Constants.DUPLICATES_ACROSS_TENANTS;
import static org.folio.dew.utils.Constants.MULTIPLE_MATCHES_MESSAGE;
import static org.folio.dew.utils.Constants.NO_ITEM_VIEW_PERMISSIONS;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.dew.utils.SearchIdentifierTypeResolver.getSearchIdentifierType;

import feign.FeignException;
import feign.codec.DecodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.bulkedit.jobs.TenantResolver;
import org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionsValidator;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.client.SearchClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.BatchIdsDto;
import org.folio.dew.domain.dto.ConsortiumItem;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.domain.dto.ExtendedItem;
import org.folio.dew.domain.dto.ExtendedItemCollection;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.ConsortiaService;
import org.folio.dew.service.FolioExecutionContextManager;
import org.folio.dew.utils.ExceptionHelper;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class ItemFetcher extends FolioExecutionContextManager implements ItemProcessor<ItemIdentifier, ExtendedItemCollection> {
  private final InventoryClient inventoryClient;
  private final ConsortiaService consortiaService;
  private final SearchClient searchClient;
  private final UserClient userClient;
  private final PermissionsValidator permissionsValidator;
  private final FolioExecutionContext folioExecutionContext;
  private final TenantResolver tenantResolver;

  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;
  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  private Set<ItemIdentifier> identifiersToCheckDuplication = ConcurrentHashMap.newKeySet();

  @Override
  public synchronized ExtendedItemCollection process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry", ErrorType.WARNING);
    }
    identifiersToCheckDuplication.add(itemIdentifier);
    var type = IdentifierType.fromValue(identifierType);
    var limit = HOLDINGS_RECORD_ID == type ? Integer.MAX_VALUE : 1;
    var idType = resolveIdentifier(identifierType);
    var identifier = "barcode".equals(idType) ? Utils.encode(itemIdentifier.getItemId()) : itemIdentifier.getItemId();
    try {
      final ExtendedItemCollection extendedItemCollection = new ExtendedItemCollection()
        .extendedItems(new ArrayList<>())
        .totalRecords(0);
      var centralTenantId = consortiaService.getCentralTenantId();
      if (isCurrentTenantCentral(centralTenantId)) {
        // Assuming item is requested by only one identifier not a collection of identifiers
        var identifierTypeEnum = getSearchIdentifierType(type);
        var batchIdsDto = new BatchIdsDto()
          .identifierType(identifierTypeEnum)
          .identifierValues(List.of(itemIdentifier.getItemId()));
        var consortiumItemCollection = searchClient.getConsortiumItemCollection(batchIdsDto);
        if (consortiumItemCollection.getTotalRecords() > 0) {
          var tenantIds = consortiumItemCollection.getItems()
            .stream()
            .map(ConsortiumItem::getTenantId).collect(Collectors.toSet());
          if (HOLDINGSRECORDID != identifierTypeEnum && tenantIds.size() > 1) {
            throw new BulkEditException(DUPLICATES_ACROSS_TENANTS, ErrorType.ERROR);
          }
          var affiliatedPermittedTenants = tenantResolver.getAffiliatedPermittedTenantIds(EntityType.ITEM,
            jobExecution, identifierType, tenantIds, itemIdentifier);
          affiliatedPermittedTenants.forEach(tenantId -> {
            try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
              var url = format(getMatchPattern(identifierType), idType, identifier);
              var itemCollection = inventoryClient.getItemByQuery(url, Integer.MAX_VALUE);
              if (itemCollection.getItems().size() > limit) {
                log.error("Central tenant case: response from {} for tenant {}: {}", url, tenantId, getResponseAsString(itemCollection));
                throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE, ErrorType.ERROR);
              }
              extendedItemCollection.getExtendedItems().addAll(
                itemCollection.getItems().stream().map(item -> new ExtendedItem().tenantId(tenantId).entity(item)).toList()
              );
              extendedItemCollection.setTotalRecords(extendedItemCollection.getTotalRecords() + itemCollection.getTotalRecords());
            } catch (Exception e) {
              log.error(e.getMessage());
              throw e;
            }
          });
        } else {
          throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
        }
      } else {
        // Process local tenant case
        checkReadPermissions(folioExecutionContext.getTenantId(), identifier);
        var url = format(getMatchPattern(identifierType), idType, identifier);
        var currentTenantId = folioExecutionContext.getTenantId();
        try {
          var itemCollection = inventoryClient.getItemByQuery(url, Integer.MAX_VALUE);
          if (itemCollection.getItems().size() > limit) {
            log.error("Member/local tenant case: response from {} for tenant {}: {}", url, currentTenantId, getResponseAsString(itemCollection));
            throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE, ErrorType.ERROR);
          }
          extendedItemCollection.setExtendedItems(itemCollection.getItems().stream()
            .map(item -> new ExtendedItem().tenantId(folioExecutionContext.getTenantId()).entity(item)).toList());
          extendedItemCollection.setTotalRecords(itemCollection.getTotalRecords());
        } catch (DecodeException e) {
          throw e;
        } catch (FeignException e) {
          log.error(e);
          throw new BulkEditException(CANNOT_GET_ITEM_FROM_INVENTORY_THROUGH_QUERY.formatted(identifier, url), ErrorType.ERROR);
        }
        if (extendedItemCollection.getExtendedItems().isEmpty()) {
          log.error(NO_MATCH_FOUND_MESSAGE);
          throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
        }
      }
      return extendedItemCollection;
    } catch (DecodeException e) {
      throw new BulkEditException(ExceptionHelper.fetchMessage(e), ErrorType.ERROR);
    }
  }

  private void checkReadPermissions(String tenantId, String identifier) {
    if (!permissionsValidator.isBulkEditReadPermissionExists(tenantId, EntityType.ITEM)) {
      var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
      throw new BulkEditException(format(NO_ITEM_VIEW_PERMISSIONS, user.getUsername(), resolveIdentifier(identifierType), identifier, tenantId), ErrorType.ERROR);
    }
  }

  private boolean isCurrentTenantCentral(String centralTenantId) {
    return StringUtils.isNotEmpty(centralTenantId) && centralTenantId.equals(folioExecutionContext.getTenantId());
  }
}
