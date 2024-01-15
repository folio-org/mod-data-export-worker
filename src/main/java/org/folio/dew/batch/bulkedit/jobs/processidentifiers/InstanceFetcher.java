package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import feign.codec.DecodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.InventoryInstancesClient;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.InstanceCollection;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.InstanceReferenceService;
import org.folio.dew.utils.ExceptionHelper;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

import static org.folio.dew.domain.dto.IdentifierType.HOLDINGS_RECORD_ID;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class InstanceFetcher implements ItemProcessor<ItemIdentifier, InstanceCollection> {
  private final InventoryInstancesClient inventoryInstancesClient;
  private final InstanceReferenceService instanceReferenceService;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  private final Set<ItemIdentifier> identifiersToCheckDuplication = new HashSet<>();

  @Override
  public InstanceCollection process(@NotNull ItemIdentifier itemIdentifier) throws BulkEditException {
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry");
    }
    identifiersToCheckDuplication.add(itemIdentifier);
    var limit = HOLDINGS_RECORD_ID == IdentifierType.fromValue(identifierType) ? Integer.MAX_VALUE : 1;
    var idType = resolveIdentifier(identifierType);
    if ("ISSN".equals(idType) || "ISBN".equals(idType)){
      String typeOfIdentifiersId = instanceReferenceService.getTypeOfIdentifiersIdByName(idType);

      return inventoryInstancesClient.getInstanceByQuery(String.format("(identifiers=/@identifierTypeId=%s \"%s\")", typeOfIdentifiersId, itemIdentifier.getItemId()));
    }
    try {
      return inventoryInstancesClient.getInstanceByQuery(String.format(getMatchPattern(identifierType), idType, itemIdentifier.getItemId()), limit);
    } catch (DecodeException e) {
      throw new BulkEditException(ExceptionHelper.fetchMessage(e));
    }
  }
}
