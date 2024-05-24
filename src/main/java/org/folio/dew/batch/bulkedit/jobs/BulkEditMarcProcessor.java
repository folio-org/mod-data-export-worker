package org.folio.dew.batch.bulkedit.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.InventoryInstancesClient;
import org.folio.dew.client.SrsClient;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.InstanceCollection;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.service.InstanceReferenceService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static org.folio.dew.domain.dto.IdentifierType.ISBN;
import static org.folio.dew.domain.dto.IdentifierType.ISSN;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditMarcProcessor implements ItemProcessor<ItemIdentifier, List<String>> {

  private static final String EXACT_MATCH_PATTERN_FOR_MARC = "%s==%s AND source==MARC";

  private final InventoryInstancesClient inventoryInstancesClient;
  private final SrsClient srsClient;

  private final InstanceReferenceService instanceReferenceService;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  @Override
  public List<String> process(ItemIdentifier itemIdentifier) throws Exception {
    var instances = getMarcInstances(itemIdentifier);
    log.info("MARC instances found: {}", instances.getTotalRecords());
    return instances.getInstances().stream().map(inst -> getMarcContent(inst.getId())).filter(Objects::nonNull).toList();
  }

  private InstanceCollection getMarcInstances(ItemIdentifier itemIdentifier) {
    return switch (IdentifierType.fromValue(identifierType)) {
      case ID, HRID ->
        inventoryInstancesClient.getInstanceByQuery(String.format(EXACT_MATCH_PATTERN_FOR_MARC, resolveIdentifier(identifierType), itemIdentifier.getItemId()), 1);
      case ISBN -> getInstancesByIdentifierTypeAndValue(ISBN, itemIdentifier.getItemId());
      case ISSN -> getInstancesByIdentifierTypeAndValue(ISSN, itemIdentifier.getItemId());
      default -> new InstanceCollection(); // Exception was thrown in the first step (bulkEditInstanceStep).
    };
  }

  private InstanceCollection getInstancesByIdentifierTypeAndValue(IdentifierType identifierType, String value) {
    return inventoryInstancesClient.getInstanceByQuery(String.format("(identifiers=/@identifierTypeId=%s \"%s\" AND source==MARC)",
      instanceReferenceService.getTypeOfIdentifiersIdByName(identifierType.getValue()), value), Integer.MAX_VALUE);
  }

  private String getMarcContent(String id) {
    var srsRecords = srsClient.getMarc(id, "INSTANCE");
    if (srsRecords.getSourceRecords().isEmpty()) {
      log.warn("No SRS records found by instanceId = {}", id);
      return null;
    }
    var recordId = srsRecords.getSourceRecords().get(0).getRecordId();
    var marcRecord = srsClient.getMarcContent(recordId);
    log.info("MARC record found by recordId = {}", recordId);
    return marcRecord.getRawRecord().getContent();
  }
}
