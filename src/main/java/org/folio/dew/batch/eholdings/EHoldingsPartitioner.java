package org.folio.dew.batch.eholdings;

import org.folio.dew.batch.CsvPartitioner;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;

public class EHoldingsPartitioner extends CsvPartitioner {

  private final String recordId;
  private final String titlesSearchFilters;
  private final KbEbscoClient kbEbscoClient;
  private final EHoldingsExportConfig.RecordTypeEnum recordType;

  public EHoldingsPartitioner(Long offset, Long limit, String tempOutputFilePath,
                              KbEbscoClient kbEbscoClient, String recordId, String recordType,
                              String titlesSearchFilters) {
    super(offset, limit, tempOutputFilePath);

    this.recordId = recordId;
    this.kbEbscoClient = kbEbscoClient;
    this.titlesSearchFilters = titlesSearchFilters;
    this.recordType = EHoldingsExportConfig.RecordTypeEnum.valueOf(recordType);
  }

  @Override
  protected Long getLimit() {
    if (recordType == EHoldingsExportConfig.RecordTypeEnum.RESOURCE) {
      return 1L;
    } else if (recordType == EHoldingsExportConfig.RecordTypeEnum.PACKAGE) {
//    return Long.valueOf(kbEbscoClient.getPackageById(recordId).getTotalTitles());
    }
    return null;
  }

}
