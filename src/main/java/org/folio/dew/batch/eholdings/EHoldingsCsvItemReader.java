package org.folio.dew.batch.eholdings;

import java.util.List;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.EHoldingsExportFormat;

public class EHoldingsCsvItemReader extends CsvItemReader<EHoldingsExportFormat> {

  private final KbEbscoClient kbEbscoClient;
  private final String recordId;
  private final String titlesSearchFilters;
  private final EHoldingsExportConfig.RecordTypeEnum recordType;
  private final JsonToEHoldingsExportFormatMapper mapper;

  protected EHoldingsCsvItemReader(Long offset, Long limit, KbEbscoClient kbEbscoClient,
                                   String recordId, String recordType, String titlesSearchFilters) {
    super(offset, limit);
    this.recordId = recordId;
    this.kbEbscoClient = kbEbscoClient;
    this.titlesSearchFilters = titlesSearchFilters;
    this.mapper = new JsonToEHoldingsExportFormatMapper();
    this.recordType = EHoldingsExportConfig.RecordTypeEnum.valueOf(recordType);
  }

  @Override
  protected List<EHoldingsExportFormat> getItems(int offset, int limit) {
    var eHoldingsExportFormat = new EHoldingsExportFormat();

    if (recordType == EHoldingsExportConfig.RecordTypeEnum.RESOURCE) {
      var packageId = recordId.split("-")[1];
      var titleId = recordId.split("-")[2];

      var packageById = kbEbscoClient.getPackageById(packageId).getBody();
      var titleById = kbEbscoClient.getTitleById(titleId).getBody();

      eHoldingsExportFormat = mapper.convertPackageToExportFormat(packageById);
      eHoldingsExportFormat.setTitles(List.of(mapper.convertTitleToExportFormat(titleById)));
    } else if (recordType == EHoldingsExportConfig.RecordTypeEnum.PACKAGE) {
      var packageById = kbEbscoClient.getPackageById(recordId).getBody();
      var titles = kbEbscoClient.getResourcesByPackageId(recordId, titlesSearchFilters).getBody();

      eHoldingsExportFormat = mapper.convertPackageToExportFormat(packageById);
      //TODO: should get tiles from resources
      eHoldingsExportFormat.setTitles(List.of(mapper.convertTitleToExportFormat(titles)));
    }

    return List.of(eHoldingsExportFormat);
  }
}
