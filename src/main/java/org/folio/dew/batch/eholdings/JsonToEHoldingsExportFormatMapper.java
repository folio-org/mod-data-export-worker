package org.folio.dew.batch.eholdings;

import java.util.List;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import org.folio.dew.domain.dto.EHoldingsExportFormat;
import org.folio.dew.domain.dto.EHoldingsTitleExportFormat;

@Component
public class JsonToEHoldingsExportFormatMapper {

  public EHoldingsExportFormat convertPackageToExportFormat(String ePackage) {
    var eHoldingsExportFormat = new EHoldingsExportFormat();
    var json = new JSONObject(ePackage);

    eHoldingsExportFormat.setProviderId(json.getString(""));
    eHoldingsExportFormat.setProviderName(json.getString(""));
    eHoldingsExportFormat.setPackageId(json.getString(""));
    eHoldingsExportFormat.setPackageName(json.getString(""));
    eHoldingsExportFormat.setPackageType(json.getString(""));
    eHoldingsExportFormat.setPackageContentType(json.getString(""));
    eHoldingsExportFormat.setPackageStartCustomCoverage(json.getString(""));
    eHoldingsExportFormat.setPackageEndCustomCoverage(json.getString(""));
    eHoldingsExportFormat.setPackageShowToPatrons(json.getString(""));
    eHoldingsExportFormat.setPackageAutomaticallySelect(json.getString(""));
    eHoldingsExportFormat.setPackageProxy(json.getString(""));
    eHoldingsExportFormat.setPackageAccessStatusType(json.getString(""));
    eHoldingsExportFormat.setPackageAgreementStartDate(json.getString(""));
    eHoldingsExportFormat.setPackageAgreementName(json.getString(""));
    eHoldingsExportFormat.setPackageAgreementStatus(json.getString(""));
    eHoldingsExportFormat.setPackageNoteLastUpdatedDate(json.getString(""));
    eHoldingsExportFormat.setPackageNoteType(json.getString(""));
    eHoldingsExportFormat.setPackageNoteTitle(json.getString(""));
    eHoldingsExportFormat.setPackageNoteDetails(json.getString(""));

    return eHoldingsExportFormat;
  }

  public EHoldingsTitleExportFormat convertTitleToExportFormat(String eTitle) {
    var eHoldingsTitleExportFormat = new EHoldingsTitleExportFormat();
    var json = new JSONObject(eTitle);

    eHoldingsTitleExportFormat.setPackageName(json.getString("data.attributes.packageName"));
    eHoldingsTitleExportFormat.setPackageId(json.getString("data.attributes.packageId"));
    eHoldingsTitleExportFormat.setTitleName(json.getString("data.attributes.name"));
    eHoldingsTitleExportFormat.setAlternateTitles(json.getJSONArray("data.attributes.alternateTitles").toList());
    eHoldingsTitleExportFormat.setTitleId(json.getString("data.id"));
//    Can't find field
//    eHoldingsTitleExportFormat.setTitleHoldingsStatus(json.getString(""));
    eHoldingsTitleExportFormat.setPublicationType(json.getString("data.attributes.publicationType"));
    eHoldingsTitleExportFormat.setTitleType(json.getString("data.type"));
    eHoldingsTitleExportFormat.setContributors(json.getJSONArray("data.attributes.contributors").toList());
    eHoldingsTitleExportFormat.setEdition(json.getString("data.attributes.edition"));
    eHoldingsTitleExportFormat.setPublisher(json.getString("data.attributes.publisherName"));
//    Can't find fields
//    eHoldingsTitleExportFormat.setISBN_Print(List.of(json.getString("")));
//    eHoldingsTitleExportFormat.setISBN_Online(List.of(json.getString("")));
//    eHoldingsTitleExportFormat.setISSN_Print(List.of(json.getString("")));
//    eHoldingsTitleExportFormat.setISSN_Online(List.of(json.getString("")));
    eHoldingsTitleExportFormat.setPeerReviewed(json.getString("data.attributes.isPeerReviewed"));
    eHoldingsTitleExportFormat.setDescription(json.getString("data.attributes.description"));
    eHoldingsTitleExportFormat.setTitle(json.getString("data.attributes.title"));
    eHoldingsTitleExportFormat.setManagedCoverage(json.getJSONArray("data.attributes.managedCoverage").toList());
    eHoldingsTitleExportFormat.setCustomCoverage(json.getJSONArray("data.attributes.customCoverage").toList());
    eHoldingsTitleExportFormat.setCoverageStatement(json.getString("data.attributes.coverageStatement"));
    eHoldingsTitleExportFormat.setManagedEmbargo(json.getString("data.attributes.managedEmbargoPeriod"));
    eHoldingsTitleExportFormat.setCustomEmbargo(json.getString("data.attributes.customEmbargoPeriod"));
//    Can't find field
//    eHoldingsTitleExportFormat.setTitleShowToPatrons(json.getString(""));
    eHoldingsTitleExportFormat.setTitleProxy(json.getString("data.attributes.proxy"));
    eHoldingsTitleExportFormat.setUrl(json.getString("data.attributes.url"));
    eHoldingsTitleExportFormat.setTitleAccessStatusType(json.getString("data.attributes.accessType"));
//    Can't find fields
//    eHoldingsTitleExportFormat.setCustomValue1(json.getString(""));
//    eHoldingsTitleExportFormat.setCustomValue2(json.getString(""));
//    eHoldingsTitleExportFormat.setCustomValue3(json.getString(""));
//    eHoldingsTitleExportFormat.setCustomValue4(json.getString(""));
//    eHoldingsTitleExportFormat.setCustomValue5(json.getString(""));
    eHoldingsTitleExportFormat.setTitleTags(List.of(json.getString("data.attributes.tags")));
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

}
