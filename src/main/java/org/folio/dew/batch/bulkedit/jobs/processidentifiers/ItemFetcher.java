package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.domain.dto.IdentifierType.FORMER_IDS;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class ItemFetcher implements ItemProcessor<ItemIdentifier, Item> {
  private final static String MATCH_PATTERN = "%s=%s";
  private final static String EXACT_MATCH_PATTERN = "%s==%s";

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
      var items = inventoryClient.getItemByQuery(String.format(getMatchPattern(identifierType), resolveIdentifier(identifierType), itemIdentifier.getItemId()), 1);
      if (!items.getItems().isEmpty()) {
        return items.getItems().get(0);
      }
    } catch (FeignException e) {
      // When user not found 404
    }
    log.error(NO_MATCH_FOUND_MESSAGE);
    throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
  }

  private String getMatchPattern(String identifierType) {
    return FORMER_IDS == IdentifierType.fromValue(identifierType) ? MATCH_PATTERN : EXACT_MATCH_PATTERN;
  }
}
