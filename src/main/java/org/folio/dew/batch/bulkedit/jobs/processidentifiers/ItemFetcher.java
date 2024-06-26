package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.domain.dto.IdentifierType.HOLDINGS_RECORD_ID;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.MULTIPLE_MATCHES_MESSAGE;

import feign.codec.DecodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.client.SearchClient;
import org.folio.dew.domain.dto.BatchIdsDto;
import org.folio.dew.domain.dto.ConsortiumItem;
import org.folio.dew.domain.dto.ExtendedItem;
import org.folio.dew.domain.dto.ExtendedItemCollection;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.ConsortiaService;
import org.folio.dew.service.FolioExecutionContextManager;
import org.folio.dew.utils.ExceptionHelper;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class ItemFetcher extends FolioExecutionContextManager implements ItemProcessor<ItemIdentifier, ExtendedItemCollection> {
  private final InventoryClient inventoryClient;
  private final ConsortiaService consortiaService;
  private final SearchClient searchClient;
  private final FolioExecutionContext folioExecutionContext;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  private Set<ItemIdentifier> identifiersToCheckDuplication = new HashSet<>();

  @Override
  public ExtendedItemCollection process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry");
    }
    identifiersToCheckDuplication.add(itemIdentifier);
    var limit = HOLDINGS_RECORD_ID == IdentifierType.fromValue(identifierType) ? Integer.MAX_VALUE : 1;
    var idType = resolveIdentifier(identifierType);
    var identifier = "barcode".equals(idType) ? Utils.encode(itemIdentifier.getItemId()) : itemIdentifier.getItemId();
    try {
      final ExtendedItemCollection extendedItems = new ExtendedItemCollection();
      if (StringUtils.isNotEmpty(consortiaService.getCentralTenantId())) {
        var batchIdsDto = new BatchIdsDto().ids(List.of(itemIdentifier.getItemId()));
        var consortiumItemCollection = searchClient.getConsortiumItemCollection(batchIdsDto);
        if (consortiumItemCollection.getTotalRecords() > 1) {
          throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE);
        } else if (consortiumItemCollection.getTotalRecords() == 1) {
          var tenantIds = Optional.of(consortiumItemCollection.getItems())
            .orElseThrow(() -> new BulkEditException("Member tenant cannot be resolved from search response"))
            .stream()
            .map(ConsortiumItem::getTenantId).toList();
          try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantIds.get(0), folioExecutionContext))) {
            var items = inventoryClient.getItemByQuery(String.format(getMatchPattern(identifierType), idType, identifier), limit);
            extendedItems.setExtendedItems(items.getItems().stream().map(item -> new ExtendedItem().tenantId(tenantIds.get(0)).entity(item)).toList());
            extendedItems.setTotalRecords(items.getTotalRecords());
          }
        } else {
          throw new BulkEditException("Member tenant cannot be resolved: search response doesn't contain tenant");
        }
      } else {
        var items =  inventoryClient.getItemByQuery(String.format(getMatchPattern(identifierType), idType, identifier), limit);
        extendedItems.setExtendedItems(items.getItems().stream().map(item -> new ExtendedItem().tenantId(folioExecutionContext.getTenantId()).entity(item)).toList());
        extendedItems.setTotalRecords(items.getTotalRecords());
      }
      if (extendedItems.getTotalRecords() > limit) {
        throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE);
      }
      return extendedItems;
    } catch (DecodeException e) {
      throw new BulkEditException(ExceptionHelper.fetchMessage(e));
    }
  }
}
