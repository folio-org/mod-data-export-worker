package org.folio.dew.batch.eholdings;

import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.RESOURCE;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.batch.core.annotation.BeforeStep;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum;
import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.ResourcesData;

public class EHoldingsItemReader extends CsvItemReader<EHoldingsResourceExportFormat> {

  private static final int QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST = 20;
  private static final int PAGE_OFFSET_STEP = 1;

  private static final String PAGE_PARAM = "page";
  private static final String COUNT_PARAM = "count";
  private static final String INCLUDE_PARAM = "include";
  private static final String ACCESS_TYPE = "accessType";

  private final KbEbscoClient kbEbscoClient;
  private final EHoldingsToExportFormatMapper mapper;
  private final RecordTypeEnum recordType;
  private final List<String> titleFields;
  private final String titleSearchFilters;
  private final String recordId;

  private EPackage ePackage;

  protected EHoldingsItemReader(KbEbscoClient kbEbscoClient, EHoldingsExportConfig exportConfig) {
    super(1L, 1L, QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST);
    setOffsetStep(PAGE_OFFSET_STEP);
    this.mapper = new EHoldingsToExportFormatMapper();
    this.kbEbscoClient = kbEbscoClient;
    this.recordId = exportConfig.getRecordId();
    this.recordType = exportConfig.getRecordType();
    this.titleFields = exportConfig.getTitleFields();
    this.titleSearchFilters = exportConfig.getTitleSearchFilters();
  }

  @Override
  protected List<EHoldingsResourceExportFormat> getItems(int offset, int limit) {
    if (recordType == RESOURCE) {
      var resourceById = kbEbscoClient.getResourceById(recordId, ACCESS_TYPE);
      var resourceIncluded = resourceById.getIncluded();
      var resourceData = resourceById.getData();
      resourceData.setIncluded(resourceIncluded);

      return buildEHoldingsExportFormat(ePackage, List.of(resourceData));
    }

    if (recordType == PACKAGE && !titleFields.isEmpty()) {
      var packageResources = kbEbscoClient
        .getResourcesByPackageId(recordId, constructParams(offset, limit, ACCESS_TYPE));

      return buildEHoldingsExportFormat(ePackage, packageResources.getData());
    }

    return buildEHoldingsExportFormat(ePackage, Collections.emptyList());
  }

  @Override
  protected void doOpen() {
    setMaxItemCount(getTotalCount());
  }

  @BeforeStep
  public void readPackage() {
    if (recordType == RESOURCE) {
      var packageId = recordId.split("-\\d+$")[0];
      this.ePackage = kbEbscoClient.getPackageById(packageId, ACCESS_TYPE);
    }
    if (recordType == PACKAGE) {
      this.ePackage = kbEbscoClient.getPackageById(recordId, ACCESS_TYPE);
    }
  }

  private List<EHoldingsResourceExportFormat> buildEHoldingsExportFormat(EPackage ePackage, List<ResourcesData> resources) {
    return resources.stream()
      .map(data -> mapper.convertToExportFormat(ePackage, data))
      .collect(Collectors.toList());
  }

  private int getTotalCount() {
    if (recordType == PACKAGE) {
      var resources = kbEbscoClient
        .getResourcesByPackageId(recordId, constructParams(1, 1, null));
      return resources.getMeta().getTotalResults();
    } else if (recordType == RESOURCE) {
      return 1;
    } else {
      return 0;
    }
  }

  private Map<String, String> constructParams(int page, int count, String include) {
    var params = new HashMap<String, String>();
    params.put(titleSearchFilters, null);
    params.put(PAGE_PARAM, String.valueOf(page));
    params.put(COUNT_PARAM, String.valueOf(count));
    if (include != null) {
      params.put(INCLUDE_PARAM, include);
    }
    return params;
  }
}
