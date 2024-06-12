package org.folio.dew.batch.bulkedit.jobs;

import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.SearchClient;
import org.folio.dew.domain.dto.BatchIdsDto;
import org.folio.dew.domain.dto.ConsortiumItem;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemCollection;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.ConsortiaService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemListProcessor implements ItemProcessor<ItemCollection, List<ItemFormat>> {
  private final BulkEditItemProcessor bulkEditItemProcessor;
  private final SearchClient searchClient;
  private final ConsortiaService consortiaService;

  @Override
  public List<ItemFormat> process(ItemCollection items) {
    Map<String, String> itemIdTenantIdMap  = getItemIdTenanIdtMap(items);
    if (items.getItems().isEmpty()) {
      log.error(NO_MATCH_FOUND_MESSAGE);
      throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
    }
    return items.getItems().stream()
      .map(bulkEditItemProcessor::process)
      .map(item -> {
        if (Objects.nonNull(item) && !itemIdTenantIdMap.isEmpty()) {
          return item.withTenantId(itemIdTenantIdMap.get(item.getId()));
        }
        return null;
      })
      .toList();
  }

  private Map<String, String> getItemIdTenanIdtMap(ItemCollection items) {
    Map<String, String> itemIdTenantIdMap = new HashMap<>();
    if (StringUtils.isNotEmpty(consortiaService.getCentralTenantId())) {
      var ids = items.getItems().stream().map(Item::getId).toList();
      var batchIdsDto = new BatchIdsDto().ids(ids);
      var consortiumItemCollectionResponse = searchClient.getConsortiumItemCollection(batchIdsDto);
      var consortiumItemCollection = consortiumItemCollectionResponse.getBody();
      if (Objects.nonNull(consortiumItemCollection) && Objects.nonNull(consortiumItemCollection.getConsortiumItemRecords())) {
        itemIdTenantIdMap = consortiumItemCollection.getConsortiumItemRecords().stream()
          .collect(Collectors.toMap(ConsortiumItem::getId, ConsortiumItem::getTenantId));
      }
    }
    return itemIdTenantIdMap;
  }
}
