package org.folio.dew.batch.eholdings;

import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.RESOURCE;

import java.util.Collections;
import java.util.List;
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
  private static final String ACCESS_TYPE_INCLUDE = "accessType";

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
      var resourceById = kbEbscoClient.getResourceById(recordId, ACCESS_TYPE_INCLUDE);
      var resourceIncluded = resourceById.getIncluded();
      var resourceData = resourceById.getData();
      resourceData.setIncluded(resourceIncluded);

      return buildEHoldingsExportFormat(ePackage, List.of(resourceData));
    }

    if (recordType == PACKAGE && !titleFields.isEmpty()) {
      var packageResources = kbEbscoClient
        .getResourcesByPackageId(recordId, titleSearchFilters, ACCESS_TYPE_INCLUDE, offset, limit);

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
      this.ePackage = kbEbscoClient.getPackageById(packageId, ACCESS_TYPE_INCLUDE);
    }
    if (recordType == PACKAGE) {
      this.ePackage = kbEbscoClient.getPackageById(recordId, ACCESS_TYPE_INCLUDE);
    }
  }

  private List<EHoldingsResourceExportFormat> buildEHoldingsExportFormat(EPackage ePackage, List<ResourcesData> resources) {
    return resources.stream()
      .map(data -> mapper.convertToExportFormat(ePackage, data))
      .collect(Collectors.toList());
  }

  private int getTotalCount() {
    if (recordType == PACKAGE) {
      var resources = kbEbscoClient.getResourcesByPackageId(recordId, titleSearchFilters, null, 1, 1);
      return resources.getMeta().getTotalResults();
    } else if (recordType == RESOURCE) {
      return 1;
    } else {
      return 0;
    }
  }
}
