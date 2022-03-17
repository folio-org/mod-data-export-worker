package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class ItemFetcher implements ItemProcessor<ItemIdentifier, Item> {

  private final InventoryClient inventoryClient;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  private Set<ItemIdentifier> identifiersToCheckDuplication = new HashSet<>();

  @Override
  public Item process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry");
    }
    identifiersToCheckDuplication.add(itemIdentifier);
    try {
      var items = inventoryClient.getItemByQuery(String.format("%s==%s", resolveIdentifier(identifierType), itemIdentifier.getItemId()), 1);
      if (!items.getItems().isEmpty()) {
        return items.getItems().get(0);
      }
    } catch (FeignException e) {
      // When user not found 404
    }
    log.error(NO_MATCH_FOUND_MESSAGE);
    throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
  }
}
