package org.folio.dew.batch.eholdings;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import org.folio.dew.domain.dto.EHoldingsExportFormat;
import org.folio.dew.domain.dto.EHoldingsRecord;
import org.folio.dew.domain.dto.EHoldingsTitle;
import org.folio.dew.domain.dto.EHoldingsTitleExportFormat;

@Log4j2
@Component
@StepScope
@RequiredArgsConstructor
public class EHoldingsItemProcessor implements ItemProcessor<EHoldingsRecord, EHoldingsExportFormat> {

  @Override
  public EHoldingsExportFormat process(EHoldingsRecord eHoldingsRecord) throws Exception {
    var eHoldingsExportFormat = new EHoldingsExportFormat();
    eHoldingsExportFormat.setProviderId("");
    eHoldingsExportFormat.setProviderName("");
    eHoldingsExportFormat.setPackageId("");
    eHoldingsExportFormat.setPackageName("");
    eHoldingsExportFormat.setPackageType("");
    eHoldingsExportFormat.setPackageContentType("");
    eHoldingsExportFormat.setPackageStartCustomCoverage("");
    eHoldingsExportFormat.setPackageEndCustomCoverage("");
    eHoldingsExportFormat.setPackageShowToPatrons("");
    eHoldingsExportFormat.setPackageAutomaticallySelect("");
    eHoldingsExportFormat.setPackageProxy("");
    eHoldingsExportFormat.setPackageAccessStatusType("");
    eHoldingsExportFormat.setPackageAgreementStartDate("");
    eHoldingsExportFormat.setPackageAgreementName("");
    eHoldingsExportFormat.setPackageAgreementStatus("");
    eHoldingsExportFormat.setPackageNoteLastUpdatedDate("");
    eHoldingsExportFormat.setPackageNoteType("");
    eHoldingsExportFormat.setPackageNoteTitle("");
    eHoldingsExportFormat.setPackageNoteDetails("");
    eHoldingsExportFormat.setTitles(eHoldingsRecord.getTitles().stream()
      .map(this::processEHoldingsTitle)
      .collect(Collectors.toList()));

    return eHoldingsExportFormat;
  }

  private EHoldingsTitleExportFormat processEHoldingsTitle(EHoldingsTitle eHoldingsTitle) {
    var eHoldingsTitleExportFormat = new EHoldingsTitleExportFormat();
    eHoldingsTitleExportFormat.setPackageName("");
    eHoldingsTitleExportFormat.setPackageId("");
    eHoldingsTitleExportFormat.setTitleName("");
    eHoldingsTitleExportFormat.setAlternateTitles(List.of(""));
    eHoldingsTitleExportFormat.setTitleId("");
    eHoldingsTitleExportFormat.setTitleHoldingsStatus("");
    eHoldingsTitleExportFormat.setTitleHoldingsStatus("");
    eHoldingsTitleExportFormat.setPublicationType("");
    eHoldingsTitleExportFormat.setTitleType("");
    eHoldingsTitleExportFormat.setContributors(List.of(""));
    eHoldingsTitleExportFormat.setEdition("");
    eHoldingsTitleExportFormat.setPublisher("");
    eHoldingsTitleExportFormat.setISBN_Print(List.of(""));
    eHoldingsTitleExportFormat.setISBN_Online(List.of(""));
    eHoldingsTitleExportFormat.setISSN_Print(List.of(""));
    eHoldingsTitleExportFormat.setISSN_Online(List.of(""));
    eHoldingsTitleExportFormat.setPeerReviewed("");
    eHoldingsTitleExportFormat.setDescription("");
    eHoldingsTitleExportFormat.setTitle("");
    eHoldingsTitleExportFormat.setStartManagedCoverage("");
    eHoldingsTitleExportFormat.setEndManagedCoverage("");
    eHoldingsTitleExportFormat.setStartCustomCoverage(List.of(""));
    eHoldingsTitleExportFormat.setEndCustomCoverage(List.of(""));
    eHoldingsTitleExportFormat.setCoverageStatement("");
    eHoldingsTitleExportFormat.setManagedEmbargo("");
    eHoldingsTitleExportFormat.setCustomEmbargo("");
    eHoldingsTitleExportFormat.setTitleShowToPatrons("");
    eHoldingsTitleExportFormat.setTitleProxy("");
    eHoldingsTitleExportFormat.setUrl("");
    eHoldingsTitleExportFormat.setTitleAccessStatusType("");
    eHoldingsTitleExportFormat.setCustomValue1("");
    eHoldingsTitleExportFormat.setCustomValue2("");
    eHoldingsTitleExportFormat.setCustomValue3("");
    eHoldingsTitleExportFormat.setCustomValue4("");
    eHoldingsTitleExportFormat.setCustomValue5("");
    eHoldingsTitleExportFormat.setTitleTags(List.of(""));
    eHoldingsTitleExportFormat.setTitleAgreementStartDate("");
    eHoldingsTitleExportFormat.setTitleAgreementName("");
    eHoldingsTitleExportFormat.setTitleAgreementStatus("");
    eHoldingsTitleExportFormat.setTitleNoteLastUpdatedDate("");
    eHoldingsTitleExportFormat.setTitleNoteType("");
    eHoldingsTitleExportFormat.setTitleNoteTitle("");
    eHoldingsTitleExportFormat.setTitleNoteDetails("");

    return eHoldingsTitleExportFormat;
  }
}
