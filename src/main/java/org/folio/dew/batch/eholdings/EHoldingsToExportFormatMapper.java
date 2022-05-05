package org.folio.dew.batch.eholdings;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import org.folio.dew.domain.dto.EHoldingsExportFormat;
import org.folio.dew.domain.dto.EHoldingsTitleExportFormat;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EResources;
import org.folio.dew.domain.dto.eholdings.ResourcesData;
import org.folio.dew.domain.dto.eholdings.Identifier;
import org.folio.dew.domain.dto.eholdings.Identifier.TypeEnum;
import org.folio.dew.domain.dto.eholdings.Identifier.SubtypeEnum;


@Component
public class EHoldingsToExportFormatMapper {

  public EHoldingsExportFormat convertPackageToExportFormat(EPackage ePackage) {
    var eHoldingsExportFormat = new EHoldingsExportFormat();
    var packageAtr = ePackage.getData().getAttributes();

    eHoldingsExportFormat.setProviderId(packageAtr.getProviderId().toString());
    eHoldingsExportFormat.setProviderName(packageAtr.getProviderName());
    eHoldingsExportFormat.setPackageId(ePackage.getData().getId());
    eHoldingsExportFormat.setPackageName(packageAtr.getName());
    eHoldingsExportFormat.setPackageType(packageAtr.getPackageType());
    eHoldingsExportFormat.setPackageContentType(packageAtr.getContentType().getValue());
    eHoldingsExportFormat.setPackageCustomCoverage(packageAtr.getCustomCoverage().toString());
    eHoldingsExportFormat.setPackageProxy(packageAtr.getProxy().toString());
    eHoldingsExportFormat.setPackageTags(packageAtr.getTags().toString());
    eHoldingsExportFormat.setPackageShowToPatrons(getShowToPatrons(packageAtr.getVisibilityData().getIsHidden()));
    eHoldingsExportFormat.setPackageAutomaticallySelect(getAutomaticallySelect(packageAtr.getAllowKbToAddTitles()));

/*    Can't find fields
    eHoldingsExportFormat.setPackageAccessStatusType(ePackage.getData().getRelationships().getAccessType().toString());
    eHoldingsExportFormat.setPackageAgreementStartDate("");
    eHoldingsExportFormat.setPackageAgreementName("");
    eHoldingsExportFormat.setPackageAgreementStatus("");
    eHoldingsExportFormat.setPackageNoteLastUpdatedDate("");
    eHoldingsExportFormat.setPackageNoteType("");
    eHoldingsExportFormat.setPackageNoteTitle("");
    eHoldingsExportFormat.setPackageNoteDetails("");*/

    return eHoldingsExportFormat;
  }

  public EHoldingsTitleExportFormat convertResourceDataToExportFormat(ResourcesData data) {
    var eHoldingsTitleExportFormat = new EHoldingsTitleExportFormat();
    var resourcesAtr = data.getAttributes();

    eHoldingsTitleExportFormat.setTitleId(resourcesAtr.getTitleId().toString());
    eHoldingsTitleExportFormat.setTitleName(resourcesAtr.getName());
    eHoldingsTitleExportFormat.setAlternateTitles(resourcesAtr.getAlternateTitles().toString());
    eHoldingsTitleExportFormat.setTitleHoldingsStatus(getHoldingsStatus(resourcesAtr.getIsSelected()));
    eHoldingsTitleExportFormat.setPublicationType(resourcesAtr.getPublicationType().getValue());
    eHoldingsTitleExportFormat.setTitleType(getTitleType(resourcesAtr.getIsTitleCustom()));
    eHoldingsTitleExportFormat.setContributors(resourcesAtr.getContributors().toString());
    eHoldingsTitleExportFormat.setEdition(resourcesAtr.getEdition());
    eHoldingsTitleExportFormat.setPublisher(resourcesAtr.getPublisherName());
    eHoldingsTitleExportFormat.setPeerReviewed(resourcesAtr.getIsPeerReviewed().toString());
    eHoldingsTitleExportFormat.setDescription(resourcesAtr.getDescription());
    eHoldingsTitleExportFormat.setManagedCoverage(resourcesAtr.getManagedCoverages().toString());
    eHoldingsTitleExportFormat.setCustomCoverage(resourcesAtr.getCustomCoverages().toString());
    eHoldingsTitleExportFormat.setCoverageStatement(resourcesAtr.getCoverageStatement());
    eHoldingsTitleExportFormat.setManagedEmbargo(resourcesAtr.getManagedEmbargoPeriod().toString());
    eHoldingsTitleExportFormat.setCustomEmbargo(resourcesAtr.getCustomEmbargoPeriod().toString());
    eHoldingsTitleExportFormat.setTitleShowToPatrons(getShowToPatrons(resourcesAtr.getVisibilityData().getIsHidden()));
    eHoldingsTitleExportFormat.setTitleProxy(resourcesAtr.getProxy().toString());
    eHoldingsTitleExportFormat.setUrl(resourcesAtr.getUrl());
    eHoldingsTitleExportFormat.setCustomValue1(resourcesAtr.getUserDefinedField1());
    eHoldingsTitleExportFormat.setCustomValue2(resourcesAtr.getUserDefinedField2());
    eHoldingsTitleExportFormat.setCustomValue3(resourcesAtr.getUserDefinedField3());
    eHoldingsTitleExportFormat.setCustomValue4(resourcesAtr.getUserDefinedField4());
    eHoldingsTitleExportFormat.setCustomValue5(resourcesAtr.getUserDefinedField5());
    eHoldingsTitleExportFormat.setTitleTags(resourcesAtr.getTags().toString());
    eHoldingsTitleExportFormat.setISBN_Print(
      getIdentifierId(resourcesAtr.getIdentifiers(), TypeEnum.ISBN, SubtypeEnum.PRINT));
    eHoldingsTitleExportFormat.setISBN_Online(
      getIdentifierId(resourcesAtr.getIdentifiers(), TypeEnum.ISBN, SubtypeEnum.ONLINE));
    eHoldingsTitleExportFormat.setISSN_Print(
      getIdentifierId(resourcesAtr.getIdentifiers(), TypeEnum.ISSN, SubtypeEnum.PRINT));
    eHoldingsTitleExportFormat.setISSN_Online(
      getIdentifierId(resourcesAtr.getIdentifiers(), TypeEnum.ISSN, SubtypeEnum.ONLINE));

/*    Can't find fields
    eHoldingsTitleExportFormat.setTitleAccessStatusType("");
    eHoldingsTitleExportFormat.setTitleAgreementStartDate(json.getString(""));
    eHoldingsTitleExportFormat.setTitleAgreementName(json.getString(""));
    eHoldingsTitleExportFormat.setTitleAgreementStatus(json.getString(""));
    eHoldingsTitleExportFormat.setTitleNoteLastUpdatedDate(json.getString(""));
    eHoldingsTitleExportFormat.setTitleNoteType(json.getString(""));
    eHoldingsTitleExportFormat.setTitleNoteTitle(json.getString(""));
    eHoldingsTitleExportFormat.setTitleNoteDetails(json.getString(""));*/

    return eHoldingsTitleExportFormat;
  }

  public List<EHoldingsTitleExportFormat> convertResourcesToExportFormat(EResources eResources) {
    return eResources.getData().stream()
      .map(this::convertResourceDataToExportFormat)
      .collect(Collectors.toList());
  }

  private String getIdentifierId(List<Identifier> identifiers, TypeEnum type, SubtypeEnum subtype) {
    var idOptional = identifiers.stream()
      .filter(identifier -> identifier.getType().equals(type))
      .filter(identifier -> identifier.getSubtype().equals(subtype))
      .map(Identifier::getId)
      .findFirst();

    return idOptional.orElse("");
  }

  private String getShowToPatrons(boolean isHidden) {
    return isHidden ? "No" : "Yes";
  }

  private String getAutomaticallySelect(boolean isAllowKbToAddTitles) {
    return isAllowKbToAddTitles ? "Yes" : "No";
  }

  private String getHoldingsStatus(boolean isSelected) {
    return isSelected ? "Selected" : "Not selected";
  }

  private String getTitleType(boolean isCustom) {
    return isCustom ? "Custom" : "Managed";
  }
}
