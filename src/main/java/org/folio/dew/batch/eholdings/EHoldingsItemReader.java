package org.folio.dew.batch.eholdings;

import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.RESOURCE;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum;
import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.ResourcesData;

public class EHoldingsItemReader extends CsvItemReader<EHoldingsResourceExportFormat> {

  private static final String ACCESS_TYPE_INCLUDE = "accessType";

  private final String titleFields;
  private final KbEbscoClient kbEbscoClient;
  private final EHoldingsToExportFormatMapper mapper;

  private String recordId;
  private String titlesSearchFilters;
  private RecordTypeEnum recordType;
  private EPackage ePackage;

  protected EHoldingsItemReader(KbEbscoClient kbEbscoClient, String titleFields) {
    super(0L, 1L);
    this.kbEbscoClient = kbEbscoClient;
    this.titleFields = titleFields;
    this.mapper = new EHoldingsToExportFormatMapper();
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

    if (recordType == PACKAGE && !titleFields.isBlank()) {
      var packageResources = kbEbscoClient
        .getResourcesByPackageId(recordId, titlesSearchFilters, ACCESS_TYPE_INCLUDE, offset, limit);

      return buildEHoldingsExportFormat(ePackage, packageResources.getData());
    }

    return buildEHoldingsExportFormat(ePackage, Collections.emptyList());
  }

  @Override
  protected void doOpen() {
    setMaxItemCount(getTotalCount());
  }

  @BeforeStep
  public void readPackage(StepExecution stepExecution) {
    this.recordId = getContextValue(stepExecution, "recordId");
    this.titlesSearchFilters = getContextValue(stepExecution, "titlesSearchFilters");
    this.recordType = RecordTypeEnum.valueOf(getContextValue(stepExecution, "recordType"));
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
      var resources = kbEbscoClient.getResourcesByPackageId(recordId, titlesSearchFilters, null, 1, 1);
      return resources.getMeta().getTotalResults();
    } else if (recordType == RESOURCE) {
      return 1;
    } else {
      return 0;
    }
  }

  private String getContextValue(StepExecution stepExecution, String name) {
    return stepExecution.getJobParameters().getString(name);
  }
}
