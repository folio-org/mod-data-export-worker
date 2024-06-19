package org.folio.dew.batch.bulkedit.jobs;

import static org.folio.dew.domain.dto.IdentifierType.HRID;
import static org.folio.dew.domain.dto.IdentifierType.ID;
import static org.folio.dew.domain.dto.IdentifierType.INSTANCE_HRID;
import static org.folio.dew.domain.dto.IdentifierType.ITEM_BARCODE;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.MULTIPLE_MATCHES_MESSAGE;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.client.SearchClient;
import org.folio.dew.domain.dto.BatchIdsDto;
import org.folio.dew.domain.dto.ConsortiumHolding;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.folio.dew.domain.dto.HoldingsRecordCollection;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.ConsortiaService;
import org.folio.dew.service.HoldingsReferenceService;
import org.folio.dew.service.mapper.HoldingsMapper;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditHoldingsProcessor implements ItemProcessor<ItemIdentifier, List<HoldingsFormat>> {
  private final HoldingClient holdingClient;
  private final HoldingsMapper holdingsMapper;
  private final HoldingsReferenceService holdingsReferenceService;
  private final SearchClient searchClient;
  private final ConsortiaService consortiaService;
  private final FolioExecutionContext folioExecutionContext;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;

  private Set<ItemIdentifier> identifiersToCheckDuplication = new HashSet<>();
  private Set<String> fetchedHoldingsIds = new HashSet<>();

  @Override
  public List<HoldingsFormat> process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry");
    }
    identifiersToCheckDuplication.add(itemIdentifier);

    var holdings = getHoldingsRecords(itemIdentifier);
    if (holdings.getHoldingsRecords().isEmpty()) {
      log.error(NO_MATCH_FOUND_MESSAGE);
      throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
    }

    var distinctHoldings = holdings.getHoldingsRecords().stream()
      .filter(holdingsRecord -> !fetchedHoldingsIds.contains(holdingsRecord.getId()))
      .toList();
    fetchedHoldingsIds.addAll(distinctHoldings.stream().map(HoldingsRecord::getId).toList());

    var instanceHrid = INSTANCE_HRID == IdentifierType.fromValue(identifierType) ? itemIdentifier.getItemId() : null;
    var itemBarcode = ITEM_BARCODE == IdentifierType.fromValue(identifierType) ? itemIdentifier.getItemId() : null;

    Map<String, String> holdingIdTenantIdMap = getHoldingIdTenantIdMap(fetchedHoldingsIds);


    return distinctHoldings.stream()
      .map(r -> holdingsMapper.mapToHoldingsFormat(r, itemIdentifier.getItemId(), jobId, FilenameUtils.getName(fileName)).withOriginal(r))
      .map(holdingsFormat -> holdingsFormat.withInstanceHrid(instanceHrid))
      .map(holdingsFormat -> holdingsFormat.withItemBarcode(itemBarcode))
      .map(holdingsFormat -> holdingsFormat.withTenantId(holdingIdTenantIdMap.get(holdingsFormat.getId())))
      .toList();
  }

  private Map<String, String> getHoldingIdTenantIdMap(Set<String> fetchedHoldingsIds) {
    Map<String, String> holdingIdTenantIdMap = new HashMap<>();

    if (StringUtils.isNotEmpty(consortiaService.getCentralTenantId())) {
      var batchIdsDto = new BatchIdsDto().ids(new ArrayList<>(fetchedHoldingsIds));

      var consortiumHoldingCollection = searchClient.getConsortiumHoldingCollection(batchIdsDto);
      if (Objects.nonNull(consortiumHoldingCollection) && Objects.nonNull(consortiumHoldingCollection.getConsortiumHoldingRecords())) {
        holdingIdTenantIdMap = consortiumHoldingCollection.getConsortiumHoldingRecords().stream()
          .collect(Collectors.toMap(ConsortiumHolding::getId, ConsortiumHolding::getTenantId));
      }

    }
    return holdingIdTenantIdMap;
  }

  private HoldingsRecordCollection getHoldingsRecords(ItemIdentifier itemIdentifier) {
    HoldingsRecordCollection holdingsRecordCollection;
    var type = IdentifierType.fromValue(identifierType);

    if (StringUtils.isNotEmpty(consortiaService.getCentralTenantId())) {
      var tenantId = searchClient.getConsortiumHoldingCollection(new BatchIdsDto().ids(List.of(itemIdentifier.getItemId()))).getConsortiumHoldingRecords()
        .stream()
        .map(ConsortiumHolding::getTenantId).findFirst();

      if (tenantId.isPresent()) {
        var headers = folioExecutionContext.getAllHeaders();
        headers.put("x-okapi-tenant", List.of(tenantId.get()));
        try (var context = new FolioExecutionContextSetter(new DefaultFolioExecutionContext(folioExecutionContext.getFolioModuleMetadata(), headers))) {
          if (ID == type || HRID == type) {
            holdingsRecordCollection = checkDuplicates(holdingClient.getHoldingsByQuery(
              String.format(getMatchPattern(identifierType), resolveIdentifier(identifierType), itemIdentifier.getItemId())));
          } else if (INSTANCE_HRID == type) {
            holdingsRecordCollection = holdingClient.getHoldingsByQuery("instanceId==" + holdingsReferenceService.getInstanceIdByHrid(itemIdentifier.getItemId()), Integer.MAX_VALUE);
          } else if (ITEM_BARCODE == type) {
            holdingsRecordCollection = holdingClient.getHoldingsByQuery("id==" + holdingsReferenceService.getHoldingsIdByItemBarcode(itemIdentifier.getItemId()), 1);
          } else {
            throw new BulkEditException(String.format("Identifier type \"%s\" is not supported", identifierType));
          }
        }
      } else {
        throw new BulkEditException("Member tenant cannot be resolved");
      }
    } else {
      if (ID == type || HRID == type) {
        holdingsRecordCollection = checkDuplicates(holdingClient.getHoldingsByQuery(
          String.format(getMatchPattern(identifierType), resolveIdentifier(identifierType), itemIdentifier.getItemId())));
      } else if (INSTANCE_HRID == type) {
        holdingsRecordCollection = holdingClient.getHoldingsByQuery("instanceId==" + holdingsReferenceService.getInstanceIdByHrid(itemIdentifier.getItemId()), Integer.MAX_VALUE);
      } else if (ITEM_BARCODE == type) {
        holdingsRecordCollection = holdingClient.getHoldingsByQuery("id==" + holdingsReferenceService.getHoldingsIdByItemBarcode(itemIdentifier.getItemId()), 1);
      } else {
        throw new BulkEditException(String.format("Identifier type \"%s\" is not supported", identifierType));
      }
    }
    return holdingsRecordCollection;
  }

  private HoldingsRecordCollection checkDuplicates(HoldingsRecordCollection holdingsRecordCollection) {
    if (holdingsRecordCollection.getTotalRecords() > 1) {
      throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE);
    }
    return holdingsRecordCollection;
  }
}
