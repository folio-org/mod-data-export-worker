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
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.ItemCollection;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.ConsortiaService;
import org.folio.dew.utils.ExceptionHelper;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class ItemFetcher implements ItemProcessor<ItemIdentifier, ItemCollection> {
  private final InventoryClient inventoryClient;
  private final ConsortiaService consortiaService;
  private final SearchClient searchClient;
  private final FolioExecutionContext folioExecutionContext;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  private Set<ItemIdentifier> identifiersToCheckDuplication = new HashSet<>();

  @Override
  public ItemCollection process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry");
    }
    identifiersToCheckDuplication.add(itemIdentifier);
    var limit = HOLDINGS_RECORD_ID == IdentifierType.fromValue(identifierType) ? Integer.MAX_VALUE : 1;
    var idType = resolveIdentifier(identifierType);
    var identifier = "barcode".equals(idType) ? Utils.encode(itemIdentifier.getItemId()) : itemIdentifier.getItemId();
    try {
      final ItemCollection items;
      if (StringUtils.isNotEmpty(consortiaService.getCentralTenantId())) {
        var tenantId = searchClient.getConsortiumItemCollection(new BatchIdsDto().ids(List.of(itemIdentifier.getItemId()))).getConsortiumItemRecords()
          .stream()
          .map(ConsortiumItem::getTenantId).findFirst();
        if (tenantId.isPresent()) {
          var headers = folioExecutionContext.getAllHeaders();
          headers.put("x-okapi-tenant", List.of(tenantId.get()));
          try (var context = new FolioExecutionContextSetter(new DefaultFolioExecutionContext(folioExecutionContext.getFolioModuleMetadata(), headers))) {
            items = inventoryClient.getItemByQuery(String.format(getMatchPattern(identifierType), idType, identifier), limit);
          }
        } else {
          throw new BulkEditException("Member tenant cannot be resolved");
        }
      } else {
        items =  inventoryClient.getItemByQuery(String.format(getMatchPattern(identifierType), idType, identifier), limit);
      }

      if (items.getTotalRecords() > limit) {
        throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE);
      }

      return items;
    } catch (DecodeException e) {
      throw new BulkEditException(ExceptionHelper.fetchMessage(e));
    }
  }
}
