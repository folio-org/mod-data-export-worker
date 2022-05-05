package org.folio.dew.batch.eholdings;

import java.util.List;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.EHoldingsExportFormat;

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
    var eHoldingsExportFormat = new EHoldingsExportFormat();

    if (recordType == EHoldingsExportConfig.RecordTypeEnum.RESOURCE) {
      var packageId = recordId.split("-\\d+$")[0];
      var resourceById = kbEbscoClient.getResourceById(recordId);
      var packageById = kbEbscoClient.getPackageById(packageId);

      eHoldingsExportFormat = mapper.convertPackageToExportFormat(packageById);
      eHoldingsExportFormat.setTitles(List.of(mapper.convertResourceDataToExportFormat(resourceById.getData())));
    } else if (recordType == EHoldingsExportConfig.RecordTypeEnum.PACKAGE) {
      var packageById = kbEbscoClient.getPackageById(recordId);
      eHoldingsExportFormat = mapper.convertPackageToExportFormat(packageById);

      if (titleFields != null && !titleFields.isBlank()) {
        var titles = kbEbscoClient.getResourcesByPackageId(recordId, titlesSearchFilters);
        eHoldingsExportFormat.setTitles(mapper.convertResourcesToExportFormat(titles));
      }
    }

    return List.of(eHoldingsExportFormat);
  }
}
