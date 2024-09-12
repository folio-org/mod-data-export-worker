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
import static org.folio.dew.utils.Constants.NO_HOLDING_AFFILIATION;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.dew.utils.SearchIdentifierTypeResolver.getSearchIdentifierType;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.client.SearchClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.BatchIdsDto;
import org.folio.dew.domain.dto.ConsortiumHolding;
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
public class BulkEditHoldingsProcessor extends FolioExecutionContextManager implements ItemProcessor<ItemIdentifier, List<HoldingsFormat>> {
  private final HoldingClient holdingClient;
  private final HoldingsMapper holdingsMapper;
  private final HoldingsReferenceService holdingsReferenceService;
  private final SearchClient searchClient;
  private final ConsortiaService consortiaService;
  private final FolioExecutionContext folioExecutionContext;
  private final UserClient userClient;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;

  private Set<ItemIdentifier> identifiersToCheckDuplication = ConcurrentHashMap.newKeySet();
  private Set<String> fetchedHoldingsIds = ConcurrentHashMap.newKeySet();

  @Override
  public List<HoldingsFormat> process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry");
    }
    identifiersToCheckDuplication.add(itemIdentifier);

    var holdings = getHoldingsRecords(itemIdentifier);
    if (holdings.getExtendedHoldingsRecords().isEmpty()) {
      log.error(NO_MATCH_FOUND_MESSAGE);
      throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
    }

    var distinctHoldings = holdings.getExtendedHoldingsRecords().stream()
      .filter(holdingsRecord -> !fetchedHoldingsIds.contains(holdingsRecord.getEntity().getId()))
      .toList();
    fetchedHoldingsIds.addAll(distinctHoldings.stream().map(extendedHoldingsRecord -> extendedHoldingsRecord.getEntity().getId()).toList());

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
          throw new BulkEditException(DUPLICATES_ACROSS_TENANTS);
        }
        tenantIds.forEach(tenantId -> {
          try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
            var holdingsRecordCollection = getHoldingsRecordCollection(type, itemIdentifier);
            extendedHoldingsRecordCollection.getExtendedHoldingsRecords().addAll(
              holdingsRecordCollection.getHoldingsRecords().stream()
                .map(holdingsRecord -> new ExtendedHoldingsRecord().tenantId(tenantId).entity(holdingsRecord)).toList()
            );
            extendedHoldingsRecordCollection.setTotalRecords(extendedHoldingsRecordCollection.getTotalRecords() + holdingsRecordCollection.getTotalRecords());
          } catch (Exception e) {
            if (e instanceof FeignException && ((FeignException) e).status() == 401) {
              var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
              throw new BulkEditException(format(NO_HOLDING_AFFILIATION, user.getUsername(), resolveIdentifier(identifierType), identifier, tenantId));
            } else {
              throw e;
            }
          }
        });
        return  extendedHoldingsRecordCollection;
      } else {
        throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
      }
    } else {
      // Process local tenant case
      var holdingsRecordCollection = getHoldingsRecordCollection(type, itemIdentifier);
      return new ExtendedHoldingsRecordCollection().extendedHoldingsRecords(holdingsRecordCollection.getHoldingsRecords().stream()
          .map(holdingsRecord -> new ExtendedHoldingsRecord().tenantId(folioExecutionContext.getTenantId()).entity(holdingsRecord)).toList())
        .totalRecords(holdingsRecordCollection.getTotalRecords());
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
        throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE);
      }
      return holdingsRecordCollection;
    } else if (INSTANCE_HRID == type) {
      return holdingClient.getHoldingsByQuery("instanceId==" + holdingsReferenceService.getInstanceIdByHrid(itemIdentifier.getItemId()), Integer.MAX_VALUE);
    } else if (ITEM_BARCODE == type) {
      return holdingClient.getHoldingsByQuery("id==" + holdingsReferenceService.getHoldingsIdByItemBarcode(itemIdentifier.getItemId()), 1);
    } else {
      throw new BulkEditException(format("Identifier type \"%s\" is not supported", identifierType));
    }
  }

}
