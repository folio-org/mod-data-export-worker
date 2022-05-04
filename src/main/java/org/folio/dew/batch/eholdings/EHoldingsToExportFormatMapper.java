package org.folio.dew.batch.eholdings;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import org.folio.dew.domain.dto.EHoldingsExportFormat;
import org.folio.dew.domain.dto.EHoldingsTitleExportFormat;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EResources;
import org.folio.dew.domain.dto.eholdings.ResourcesData;

@Component
public class EHoldingsToExportFormatMapper {

  public EHoldingsExportFormat convertPackageToExportFormat(EPackage ePackage) {
    var eHoldingsExportFormat = new EHoldingsExportFormat();
    var packageAtr = ePackage.getData().getAttributes();

    eHoldingsExportFormat.setProviderId(packageAtr.getProviderId().toString());
    eHoldingsExportFormat.setProviderName(packageAtr.getProviderName());
    eHoldingsExportFormat.setPackageId(ePackage.getData().getId());
    eHoldingsExportFormat.setPackageName(packageAtr.getName());
    eHoldingsExportFormat.setPackageType(ePackage.getData().getType());
    eHoldingsExportFormat.setPackageContentType(packageAtr.getContentType().getValue());
    eHoldingsExportFormat.setPackageCustomCoverage(packageAtr.getCustomCoverage().toString());
    eHoldingsExportFormat.setPackageProxy(packageAtr.getProxy().toString());
    eHoldingsExportFormat.setPackageAccessStatusType(ePackage.getData().getRelationships().getAccessType().toString());
    eHoldingsExportFormat.setPackageTags(packageAtr.getTags().toString());
    eHoldingsExportFormat.setPackageShowToPatrons("");
    eHoldingsExportFormat.setPackageAutomaticallySelect("");
    eHoldingsExportFormat.setPackageAgreementStartDate("");
    eHoldingsExportFormat.setPackageAgreementName("");
    eHoldingsExportFormat.setPackageAgreementStatus("");
    eHoldingsExportFormat.setPackageNoteLastUpdatedDate("");
    eHoldingsExportFormat.setPackageNoteType("");
    eHoldingsExportFormat.setPackageNoteTitle("");
    eHoldingsExportFormat.setPackageNoteDetails("");

    return eHoldingsExportFormat;
  }

  public EHoldingsTitleExportFormat convertResourceDataToExportFormat(ResourcesData data) {
    var eHoldingsTitleExportFormat = new EHoldingsTitleExportFormat();
    var resourcesAtr = data.getAttributes();

    eHoldingsTitleExportFormat.setTitleId(resourcesAtr.getTitleId().toString());
    eHoldingsTitleExportFormat.setTitleName(resourcesAtr.getName());
    eHoldingsTitleExportFormat.setAlternateTitles(resourcesAtr.getAlternateTitles().toString());
//    Can't find field
//    eHoldingsTitleExportFormat.setTitleHoldingsStatus(json.getString(""));
    eHoldingsTitleExportFormat.setPublicationType(resourcesAtr.getPublicationType().getValue());
    eHoldingsTitleExportFormat.setTitleType("");
    eHoldingsTitleExportFormat.setContributors(resourcesAtr.getContributors().toString());
    eHoldingsTitleExportFormat.setEdition(resourcesAtr.getEdition());
    eHoldingsTitleExportFormat.setPublisher(resourcesAtr.getPublisherName());
//    Can't find fields
//    eHoldingsTitleExportFormat.setISBN_Print(List.of(json.getString("")));
//    eHoldingsTitleExportFormat.setISBN_Online(List.of(json.getString("")));
//    eHoldingsTitleExportFormat.setISSN_Print(List.of(json.getString("")));
//    eHoldingsTitleExportFormat.setISSN_Online(List.of(json.getString("")));

    eHoldingsTitleExportFormat.setPeerReviewed(resourcesAtr.getIsPeerReviewed().toString());
    eHoldingsTitleExportFormat.setDescription(resourcesAtr.getDescription());
    eHoldingsTitleExportFormat.setManagedCoverage(resourcesAtr.getManagedCoverages().toString());
    eHoldingsTitleExportFormat.setCustomCoverage(resourcesAtr.getCustomCoverages().toString());
    eHoldingsTitleExportFormat.setCoverageStatement(resourcesAtr.getCoverageStatement());
    eHoldingsTitleExportFormat.setManagedEmbargo(resourcesAtr.getManagedEmbargoPeriod().toString());
    eHoldingsTitleExportFormat.setCustomEmbargo(resourcesAtr.getCustomEmbargoPeriod().toString());

//    Can't find field
//    eHoldingsTitleExportFormat.setTitleShowToPatrons(json.getString(""));
    eHoldingsTitleExportFormat.setTitleProxy(resourcesAtr.getProxy().toString());
    eHoldingsTitleExportFormat.setUrl(resourcesAtr.getUrl());
    eHoldingsTitleExportFormat.setTitleAccessStatusType("");
    eHoldingsTitleExportFormat.setCustomValue1(resourcesAtr.getUserDefinedField1());
    eHoldingsTitleExportFormat.setCustomValue2(resourcesAtr.getUserDefinedField2());
    eHoldingsTitleExportFormat.setCustomValue3(resourcesAtr.getUserDefinedField3());
    eHoldingsTitleExportFormat.setCustomValue4(resourcesAtr.getUserDefinedField4());
    eHoldingsTitleExportFormat.setCustomValue5(resourcesAtr.getUserDefinedField5());
    eHoldingsTitleExportFormat.setTitleTags(resourcesAtr.getTags().toString());

//    Can't find fields
//    eHoldingsTitleExportFormat.setTitleAgreementStartDate(json.getString(""));
//    eHoldingsTitleExportFormat.setTitleAgreementName(json.getString(""));
//    eHoldingsTitleExportFormat.setTitleAgreementStatus(json.getString(""));
//    eHoldingsTitleExportFormat.setTitleNoteLastUpdatedDate(json.getString(""));
//    eHoldingsTitleExportFormat.setTitleNoteType(json.getString(""));
//    eHoldingsTitleExportFormat.setTitleNoteTitle(json.getString(""));
//    eHoldingsTitleExportFormat.setTitleNoteDetails(json.getString(""));

    return eHoldingsTitleExportFormat;
  }

  public List<EHoldingsTitleExportFormat> convertResourcesToExportFormat(EResources eResources) {
    return eResources.getData().stream()
      .map(this::convertResourceDataToExportFormat)
      .collect(Collectors.toList());
  }
}
