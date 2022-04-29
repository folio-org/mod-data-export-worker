package org.folio.dew.batch.eholdings;

import java.util.List;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.EHoldingsRecord;

public class EHoldingsCsvItemReader extends CsvItemReader<EHoldingsRecord> {

  private final KbEbscoClient kbEbscoClient;
  private final String recordId;
  private final String titlesSearchFilters;
  private final EHoldingsExportConfig.RecordTypeEnum recordType;

  protected EHoldingsCsvItemReader(Long offset, Long limit, KbEbscoClient kbEbscoClient,
                                   String recordId, String recordType, String titlesSearchFilters) {
    super(offset, limit);
    this.recordId = recordId;
    this.kbEbscoClient = kbEbscoClient;
    this.titlesSearchFilters = titlesSearchFilters;
    this.recordType = EHoldingsExportConfig.RecordTypeEnum.valueOf(recordType);
  }

  @Override
  protected List<EHoldingsRecord> getItems(int offset, int limit) {
    var eHoldingsRecord = new EHoldingsRecord();

    if (recordType == EHoldingsExportConfig.RecordTypeEnum.RESOURCE) {
      var packageId = recordId.split("-")[1];
      var titleId = recordId.split("-")[2];

      eHoldingsRecord
        ._package(kbEbscoClient.getPackageById(packageId))
        .addTitlesItem(kbEbscoClient.getTitleById(titleId));
    } else if (recordType == EHoldingsExportConfig.RecordTypeEnum.PACKAGE) {
      var packageById = kbEbscoClient.getPackageById(recordId);
      var titles = kbEbscoClient.getResourcesByPackageId(recordId, titlesSearchFilters);

      eHoldingsRecord
        ._package(packageById)
        .titles(titles);
    }

    return List.of(eHoldingsRecord);
  }
}
