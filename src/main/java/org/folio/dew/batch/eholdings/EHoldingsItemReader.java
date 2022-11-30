package org.folio.dew.batch.eholdings;

import static java.util.Collections.singletonList;
import static org.folio.dew.client.KbEbscoClient.ACCESS_TYPE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.RESOURCE;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.config.properties.EHoldingsJobProperties;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.folio.dew.domain.dto.eholdings.ResourcesData;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class EHoldingsItemReader extends AbstractItemCountingItemStreamItemReader<EHoldingsResourceDTO> {

  static final int MAX_RETRIEVABLE_RESULTS = 9_999;

  private int limit;
  private int page;

  private List<EHoldingsResourceDTO> currentChunk;
  private int currentChunkOffset;

  private final KbEbscoClient kbEbscoClient;
  private final RecordTypeEnum recordType;
  private final List<String> titleFields;
  private final String titleSearchFilters;
  private final String recordId;

  protected EHoldingsItemReader(KbEbscoClient kbEbscoClient, EHoldingsExportConfig exportConfig,
                                EHoldingsJobProperties jobProperties) {
    this.page = 1;
    this.limit = jobProperties.getKbEbscoChunkSize();

    setCurrentItemCount(0);
    setSaveState(false);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());

    this.kbEbscoClient = kbEbscoClient;
    this.recordId = exportConfig.getRecordId();
    this.recordType = exportConfig.getRecordType();
    this.titleFields = exportConfig.getTitleFields();
    this.titleSearchFilters = exportConfig.getTitleSearchFilters() + "&sort=name";
  }

  @Override
  protected EHoldingsResourceDTO doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      currentChunk = getItems(page, limit);
      updatePaging();
      currentChunkOffset = 0;
    }

    if (currentChunk.isEmpty()) {
      return null;
    }

    var item = currentChunk.get(currentChunkOffset);
    currentChunkOffset++;

    return item;
  }

  protected List<EHoldingsResourceDTO> getItems(int itemPage, int itemLimit) {
    if (recordType == RESOURCE) {
      var resourceById = kbEbscoClient.getResourceById(recordId, ACCESS_TYPE);
      var resourceIncluded = resourceById.getIncluded();
      var resourceData = resourceById.getData();
      resourceData.setIncluded(resourceIncluded);

      return getEHoldingsResources(singletonList(resourceData));
    }

    if (recordType == PACKAGE && CollectionUtils.isNotEmpty(titleFields)) {
      var parameters = kbEbscoClient.constructParams(itemPage, itemLimit, titleSearchFilters, ACCESS_TYPE);
      var packageResources = kbEbscoClient.getResourcesByPackageId(recordId, parameters);

      return getEHoldingsResources(packageResources.getData());
    }

    return Collections.emptyList();
  }

  @Override
  protected void doOpen() {
    setMaxItemCount(getTotalCount());
  }

  @Override
  protected void doClose() throws Exception {
    // Nothing to do
  }

  private int getTotalCount() {
    if (recordType == PACKAGE) {
      if (CollectionUtils.isEmpty(titleFields)) {
        return 1;
      }
      var parameters = kbEbscoClient.constructParams(1, 1, titleSearchFilters);
      var resources = kbEbscoClient.getResourcesByPackageId(recordId, parameters);
      var totalResults = resources.getMeta().getTotalResults();
      return calculateTotalResults(totalResults);
    } else if (recordType == RESOURCE) {
      return 1;
    } else {
      return 0;
    }
  }

  private int calculateTotalResults(Integer totalResults) {
    if (totalResults <= 0) {
      return 1;
    }
    if (totalResults > MAX_RETRIEVABLE_RESULTS) {
      return MAX_RETRIEVABLE_RESULTS;
    }
    return totalResults;
  }

  /**
   * In case page * limit exceeds maximum retrievable results from kb - updates page, limit to retrieve up to retrievable
   * */
  private void updatePaging() {
    var plannedResults = ++page * limit;
    if (plannedResults <= MAX_RETRIEVABLE_RESULTS) {
      return;
    }

    var alreadyRetrieved = (page - 1) * limit;
    limit = BigInteger.valueOf(MAX_RETRIEVABLE_RESULTS).gcd(BigInteger.valueOf(alreadyRetrieved)).intValue();
    page = alreadyRetrieved / limit + 1;
  }

  private List<EHoldingsResourceDTO> getEHoldingsResources(List<ResourcesData> resourcesData) {
    return resourcesData.stream()
      .map(data -> EHoldingsResourceDTO.builder()
        .resourcesData(data)
        .build())
      .collect(Collectors.toList());
  }
}
