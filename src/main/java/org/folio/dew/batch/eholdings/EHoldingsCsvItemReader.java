package org.folio.dew.batch.eholdings;

import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.RESOURCE;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.EHoldingsExportFormat;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EResource;
import org.folio.dew.domain.dto.eholdings.EResources;
import org.folio.dew.domain.dto.eholdings.PackageData;

public class EHoldingsCsvItemReader extends CsvItemReader<EHoldingsExportFormat> {

  private final KbEbscoClient kbEbscoClient;
  private final String recordId;
  private final String titleFields;
  private final String titlesSearchFilters;
  private final EHoldingsExportConfig.RecordTypeEnum recordType;
  private final EHoldingsToExportFormatMapper mapper;

  protected EHoldingsCsvItemReader(Long offset, Long limit, KbEbscoClient kbEbscoClient,
                                   String recordId, String recordType, String titleFields, String titlesSearchFilters) {
    super(offset, limit);
    this.recordId = recordId;
    this.titleFields = titleFields;
    this.kbEbscoClient = kbEbscoClient;
    this.titlesSearchFilters = titlesSearchFilters;
    this.mapper = new EHoldingsToExportFormatMapper();
    this.recordType = EHoldingsExportConfig.RecordTypeEnum.valueOf(recordType);
  }

  @Override
  protected List<EHoldingsExportFormat> getItems(int offset, int limit) {
    if (recordType == RESOURCE) {
      ObjectMapper mapperO = new ObjectMapper();
      var resourceById = kbEbscoClient.getResourceById(recordId, "package");
      var includedPackage = mapperO.convertValue(resourceById.getIncluded().get(0), PackageData.class);

      return buildEHoldingsExportFormat(includedPackage, resourceById);
    }

    if (recordType == PACKAGE) {
      var packageById = kbEbscoClient.getPackageById(recordId);

      if (titleFields != null && !titleFields.isBlank()) {
        var packageResources = kbEbscoClient.getResourcesByPackageId(recordId, titlesSearchFilters);
        return buildEHoldingsExportFormat(packageById, packageResources);
      }
      return buildEHoldingsExportFormat(packageById);
    }

    return Collections.emptyList();
  }

  private List<EHoldingsExportFormat> buildEHoldingsExportFormat(PackageData packageData, EResource eResource) {
    var eHoldingsExportFormat = mapper.convertPackageToExportFormat(packageData);
    var titleInfo = mapper.convertResourceDataToExportFormat(eResource.getData());

    eHoldingsExportFormat.setTitles(List.of(titleInfo));
    return List.of(eHoldingsExportFormat);
  }

  private List<EHoldingsExportFormat> buildEHoldingsExportFormat(EPackage ePackage, EResources eResources) {
    var eHoldingsExportFormat = mapper.convertPackageToExportFormat(ePackage.getData());
    var titlesInfo = eResources.getData().stream()
      .map(mapper::convertResourceDataToExportFormat)
      .collect(Collectors.toList());

    eHoldingsExportFormat.setTitles(titlesInfo);
    return List.of(eHoldingsExportFormat);
  }

  private List<EHoldingsExportFormat> buildEHoldingsExportFormat(EPackage ePackage) {
    return List.of(mapper.convertPackageToExportFormat(ePackage.getData()));
  }
}
