package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.domain.dto.IdentifierType.HOLDINGS_RECORD_ID;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;

import feign.codec.DecodeException;
import liquibase.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.ItemCollection;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.utils.ExceptionHelper;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class ItemFetcher implements ItemProcessor<ItemIdentifier, ItemCollection> {
  private final InventoryClient inventoryClient;

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
      return inventoryClient.getItemByQuery(String.format(getMatchPattern(identifierType), idType, identifier), limit);
    } catch (DecodeException e) {
      throw new BulkEditException(ExceptionHelper.fetchMessage(e));
    }
  }
}
