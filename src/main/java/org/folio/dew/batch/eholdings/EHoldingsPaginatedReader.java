package org.folio.dew.batch.eholdings;

import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.RESOURCE;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;

import org.folio.dew.batch.AbstractPaginatedReader;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum;
import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.ResourcesData;

public class EHoldingsPaginatedReader extends AbstractPaginatedReader<EHoldingsResourceExportFormat> {

  private static final String ACCESS_TYPE_INCLUDE = "accessType";

  private final String titleFields;
  private final String titlesSearchFilters;
  private final KbEbscoClient kbEbscoClient;
  private final EHoldingsToExportFormatMapper mapper;

  private String recordId;
  private RecordTypeEnum recordType;
  private EPackage ePackage;

  protected EHoldingsPaginatedReader(KbEbscoClient kbEbscoClient, String titleFields, String titlesSearchFilters) {
    super();
    this.kbEbscoClient = kbEbscoClient;
    this.titleFields = titleFields;
    this.titlesSearchFilters = titlesSearchFilters;
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
  protected int getTotalCount() {
    if (recordType == PACKAGE) {
      var resources = kbEbscoClient.getResourcesByPackageId(recordId, titlesSearchFilters, ACCESS_TYPE_INCLUDE, 1, 1);
      return resources.getMeta().getTotalResults();
    } else if (recordType == RESOURCE) {
      return 1;
    } else {
      return 0;
    }
  }

  @BeforeStep
  public void readPackage(StepExecution stepExecution) {
    this.recordId = getContextValue(stepExecution, "recordId");
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
    var titlesInfo = resources.stream()
      .map(mapper::convertResourceDataToExportFormat)
      .collect(Collectors.toList());

    var eHoldingsExportFormat = mapper.convertPackageToExportFormat(ePackage);
    titlesInfo.forEach(resource -> resource.setEHoldingsPackageExportFormat(eHoldingsExportFormat));

    return titlesInfo;
  }

  private String getContextValue(StepExecution stepExecution, String name) {
    return (String) stepExecution.getExecutionContext().get(name);
  }
}
