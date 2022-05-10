package org.folio.dew.batch.eholdings;

import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.RESOURCE;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.EHoldingsPackageExportFormat;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EResource;
import org.folio.dew.domain.dto.eholdings.EResources;

public class EHoldingsCsvItemReader extends CsvItemReader<EHoldingsPackageExportFormat> {

  private static final String ACCESS_TYPE_INCLUDE = "accessType";

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
  protected List<EHoldingsPackageExportFormat> getItems(int offset, int limit) {
    if (recordType == RESOURCE) {
      var resourceById = kbEbscoClient.getResourceById(recordId, ACCESS_TYPE_INCLUDE);
      var packageId = resourceById.getData().getAttributes().getPackageId();
      var packageById = kbEbscoClient.getPackageById(packageId, ACCESS_TYPE_INCLUDE);

      return buildEHoldingsExportFormat(packageById, resourceById);
    }

    if (recordType == PACKAGE) {
      var packageById = kbEbscoClient.getPackageById(recordId, ACCESS_TYPE_INCLUDE);

      if (titleFields != null && !titleFields.isBlank()) {
        var packageResources = kbEbscoClient.getResourcesByPackageId(recordId, titlesSearchFilters, ACCESS_TYPE_INCLUDE);
        return buildEHoldingsExportFormat(packageById, packageResources);
      }
      return buildEHoldingsExportFormat(packageById, new EResources());
    }

    return Collections.emptyList();
  }

  private List<EHoldingsPackageExportFormat> buildEHoldingsExportFormat(EPackage ePackage, EResource eResource) {
    var eHoldingsExportFormat = mapper.convertPackageToExportFormat(ePackage);
    var titleInfo = mapper.convertResourceToExportFormat(eResource);

    eHoldingsExportFormat.setTitles(List.of(titleInfo));
    return List.of(eHoldingsExportFormat);
  }

  private List<EHoldingsPackageExportFormat> buildEHoldingsExportFormat(EPackage ePackage, EResources eResources) {
    var eHoldingsExportFormat = mapper.convertPackageToExportFormat(ePackage);

    var titlesInfo = eResources.getData().stream()
      .map(mapper::convertResourceToExportFormat)
      .collect(Collectors.toList());

    eHoldingsExportFormat.setTitles(titlesInfo);
    return List.of(eHoldingsExportFormat);
  }
}
