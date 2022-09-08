package org.folio.dew.batch.bulkedit.jobs;

import static org.folio.dew.domain.dto.IdentifierType.INSTANCE_HRID;
import static org.folio.dew.domain.dto.IdentifierType.ITEM_BARCODE;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsRecordCollection;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.HoldingsReferenceService;
import org.folio.dew.service.mapper.HoldingsMapper;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
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

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  private Set<ItemIdentifier> identifiersToCheckDuplication = new HashSet<>();

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

    var instanceHrid = INSTANCE_HRID == IdentifierType.fromValue(identifierType) ? itemIdentifier.getItemId() : null;
    var itemBarcode = ITEM_BARCODE == IdentifierType.fromValue(identifierType) ? itemIdentifier.getItemId() : null;

    return holdings.getHoldingsRecords().stream()
      .map(holdingsMapper::mapToHoldingsFormat)
      .map(holdingsFormat -> holdingsFormat.withInstanceHrid(instanceHrid))
      .map(holdingsFormat -> holdingsFormat.withItemBarcode(itemBarcode))
      .collect(Collectors.toList());
  }

  private HoldingsRecordCollection getHoldingsRecords(ItemIdentifier itemIdentifier) {
    switch (IdentifierType.fromValue(identifierType)) {
      case ID:
      case HRID:
        return holdingClient.getHoldingsByQuery(String.format(getMatchPattern(identifierType), resolveIdentifier(identifierType), itemIdentifier.getItemId()));
      case INSTANCE_HRID:
        return holdingClient.getHoldingsByQuery("instanceId==" + holdingsReferenceService.getInstanceIdByHrid(itemIdentifier.getItemId()));
      case ITEM_BARCODE:
        return holdingClient.getHoldingsByQuery("id==" + holdingsReferenceService.getHoldingsIdByItemBarcode(itemIdentifier.getItemId()), 1);
      default:
        throw new BulkEditException(String.format("Identifier type \"%s\" is not supported", identifierType));
    }
  }
}
