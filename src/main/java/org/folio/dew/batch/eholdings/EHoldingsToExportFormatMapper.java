package org.folio.dew.batch.eholdings;

import static java.util.Objects.isNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import org.folio.dew.domain.dto.EHoldingsPackageExportFormat;
import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.folio.dew.domain.dto.eholdings.AccessTypeData;
import org.folio.dew.domain.dto.eholdings.AlternateTitle;
import org.folio.dew.domain.dto.eholdings.Contributor;
import org.folio.dew.domain.dto.eholdings.Coverage;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EResource;
import org.folio.dew.domain.dto.eholdings.EmbargoPeriod;
import org.folio.dew.domain.dto.eholdings.Identifier;
import org.folio.dew.domain.dto.eholdings.Identifier.SubtypeEnum;
import org.folio.dew.domain.dto.eholdings.Identifier.TypeEnum;
import org.folio.dew.domain.dto.eholdings.PackageData;
import org.folio.dew.domain.dto.eholdings.Proxy;
import org.folio.dew.domain.dto.eholdings.ResourcesData;
import org.folio.dew.domain.dto.eholdings.Subject;
import org.folio.dew.domain.dto.eholdings.Tags;
import org.folio.dew.domain.dto.eholdings.VisibilityData;

@Component
public class EHoldingsToExportFormatMapper {

  private static final String ACCESS_TYPE_INCLUDED = "accessTypes";

  private final ObjectMapper objectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public EHoldingsPackageExportFormat convertPackageToExportFormat(EPackage ePackage) {
    var eHoldingsExportFormat = convertPackageToExportFormat(ePackage.getData());
    eHoldingsExportFormat.setPackageAccessStatusType(mapAccessType(ePackage.getIncluded()));
    return eHoldingsExportFormat;
  }

  public EHoldingsPackageExportFormat convertPackageToExportFormat(PackageData data) {
    var packageExportFormat = new EHoldingsPackageExportFormat();
    var packageAtr = data.getAttributes();

    packageExportFormat.setProviderId(packageAtr.getProviderId().toString());
    packageExportFormat.setProviderName(packageAtr.getProviderName());
    packageExportFormat.setPackageId(data.getId());
    packageExportFormat.setPackageName(packageAtr.getName());
    packageExportFormat.setPackageType(packageAtr.getPackageType());
    packageExportFormat.setPackageContentType(packageAtr.getContentType().getValue());
    packageExportFormat.setPackageCustomCoverage(mapCoverage(packageAtr.getCustomCoverage()));
    packageExportFormat.setPackageProxy(mapProxy(packageAtr.getProxy()));
    packageExportFormat.setPackageTags(mapTags(packageAtr.getTags()));
    packageExportFormat.setPackageShowToPatrons(mapShowToPatrons(packageAtr.getVisibilityData()));
    packageExportFormat.setPackageAutomaticallySelect(convertBoolToStr(packageAtr.getAllowKbToAddTitles()));

/*  Need to add mod-notes and mod-agreements integration
    packageExportFormat.setPackageAgreementStartDate("");
    packageExportFormat.setPackageAgreementName("");
    packageExportFormat.setPackageAgreementStatus("");
    packageExportFormat.setPackageNoteLastUpdatedDate("");
    packageExportFormat.setPackageNoteType("");
    packageExportFormat.setPackageNoteTitle("");
    packageExportFormat.setPackageNoteDetails("");*/

    return packageExportFormat;
  }

  public EHoldingsResourceExportFormat convertResourceToExportFormat(EResource eResource) {
    var eHoldingsExportFormat = convertResourceToExportFormat(eResource.getData());
    eHoldingsExportFormat.setTitleAccessStatusType(mapAccessType(eResource.getIncluded()));
    return eHoldingsExportFormat;
  }

  public EHoldingsResourceExportFormat convertResourceToExportFormat(ResourcesData data) {
    var titleExportFormat = new EHoldingsResourceExportFormat();
    var resourceAtr = data.getAttributes();

    titleExportFormat.setTitleId(resourceAtr.getTitleId().toString());
    titleExportFormat.setTitleName(resourceAtr.getName());
    titleExportFormat.setAlternateTitles(mapAlternateTitles(resourceAtr.getAlternateTitles()));
    titleExportFormat.setTitleHoldingsStatus(mapHoldingsStatus(resourceAtr.getIsSelected()));
    titleExportFormat.setPublicationType(resourceAtr.getPublicationType().getValue());
    titleExportFormat.setTitleType(mapTitleType(resourceAtr.getIsTitleCustom()));
    titleExportFormat.setContributors(mapContributors(resourceAtr.getContributors()));
    titleExportFormat.setEdition(resourceAtr.getEdition());
    titleExportFormat.setPublisher(resourceAtr.getPublisherName());
    titleExportFormat.setPeerReviewed(convertBoolToStr(resourceAtr.getIsPeerReviewed()));
    titleExportFormat.setDescription(resourceAtr.getDescription());
    titleExportFormat.setManagedCoverage(mapCoverage(resourceAtr.getManagedCoverages()));
    titleExportFormat.setCustomCoverage(mapCoverage(resourceAtr.getManagedCoverages()));
    titleExportFormat.setCoverageStatement(resourceAtr.getCoverageStatement());
    titleExportFormat.setManagedEmbargo(mapEmbargo(resourceAtr.getManagedEmbargoPeriod()));
    titleExportFormat.setCustomEmbargo(mapEmbargo(resourceAtr.getCustomEmbargoPeriod()));
    titleExportFormat.setTitleShowToPatrons(mapShowToPatrons(resourceAtr.getVisibilityData()));
    titleExportFormat.setTitleProxy(mapProxy(resourceAtr.getProxy()));
    titleExportFormat.setUrl(resourceAtr.getUrl());
    titleExportFormat.setSubjects(mapSubjects(resourceAtr.getSubjects()));
    titleExportFormat.setCustomValue1(resourceAtr.getUserDefinedField1());
    titleExportFormat.setCustomValue2(resourceAtr.getUserDefinedField2());
    titleExportFormat.setCustomValue3(resourceAtr.getUserDefinedField3());
    titleExportFormat.setCustomValue4(resourceAtr.getUserDefinedField4());
    titleExportFormat.setCustomValue5(resourceAtr.getUserDefinedField5());
    titleExportFormat.setTitleAccessStatusType(mapAccessType(data.getIncluded()));
    titleExportFormat.setTitleTags(mapTags(resourceAtr.getTags()));
    titleExportFormat.setISBN_Print(
      mapIdentifierId(resourceAtr.getIdentifiers(), TypeEnum.ISBN, SubtypeEnum.PRINT));
    titleExportFormat.setISBN_Online(
      mapIdentifierId(resourceAtr.getIdentifiers(), TypeEnum.ISBN, SubtypeEnum.ONLINE));
    titleExportFormat.setISSN_Print(
      mapIdentifierId(resourceAtr.getIdentifiers(), TypeEnum.ISSN, SubtypeEnum.PRINT));
    titleExportFormat.setISSN_Online(
      mapIdentifierId(resourceAtr.getIdentifiers(), TypeEnum.ISSN, SubtypeEnum.ONLINE));

/*  Need to add mod-notes and mod-agreements integration
    titleExportFormat.setTitleAgreementStartDate(json.getString(""));
    titleExportFormat.setTitleAgreementName(json.getString(""));
    titleExportFormat.setTitleAgreementStatus(json.getString(""));
    titleExportFormat.setTitleNoteLastUpdatedDate(json.getString(""));
    titleExportFormat.setTitleNoteType(json.getString(""));
    titleExportFormat.setTitleNoteTitle(json.getString(""));
    titleExportFormat.setTitleNoteDetails(json.getString(""));*/

    return titleExportFormat;
  }

  private Object getIncludedObject(List<Object> included, String type) {
    if (isNull(included)) return null;
    return included.stream()
      .map(LinkedHashMap.class::cast)
      .filter(o -> o.get("type").equals(type))
      .findFirst().orElse(null);
  }

  private String convertBoolToStr(Boolean isTrue) {
    if (isNull(isTrue)) return "";
    return isTrue ? "Yes" : "No";
  }

  private String mapAccessType(List<Object> included) {
    var accessTypeJson = getIncludedObject(included, ACCESS_TYPE_INCLUDED);
    var accessType = objectMapper.convertValue(accessTypeJson, AccessTypeData.class);
    if (accessType == null) {
      return "-";
    }
    return accessType.getAttributes().getName();
  }

  private String mapTitleType(boolean isCustom) {
    return isCustom ? "Custom" : "Managed";
  }

  private String mapHoldingsStatus(boolean isSelected) {
    return isSelected ? "Selected" : "Not selected";
  }

  private String mapTags(Tags tags) {
    if (isNull(tags)) return "";
    return String.join(", ", tags.getTagList());
  }

  private String mapEmbargo(EmbargoPeriod embargo) {
    if (embargo.getEmbargoValue() <= 0) return "";
    return embargo.getEmbargoValue() + " " + embargo.getEmbargoUnit();
  }

  private String mapProxy(Proxy proxy) {
    if (proxy.getId().equals("<n>")) {
      return "None";
    }
    var inherited = proxy.getInherited() ? "Inherited - " : "";
    return inherited + proxy.getId();
  }

  private String mapCoverage(Coverage coverage) {
    var start = coverage.getBeginCoverage().replace('-', '/');
    var end = coverage.getEndCoverage().replace('-', '/');
    if (start.isBlank()) {
      return "";
    }
    if (end.isBlank()) {
      end = "Present";
    }
    return start + " - " + end;
  }

  private List<String> mapContributors(List<Contributor> contributors) {
    return contributors.stream()
      .map(contributor -> contributor.getType() + ": " + contributor.getContributor())
      .collect(Collectors.toList());
  }

  private String mapSubjects(List<Subject> subjects) {
    return subjects.stream()
      .map(Subject::getSubject)
      .collect(Collectors.joining("; "));
  }

  private String mapCoverage(List<Coverage> coverages) {
    return coverages.stream()
      .map(this::mapCoverage)
      .collect(Collectors.joining(", "));
  }

  private String mapAlternateTitles(List<AlternateTitle> alternateTitles) {
    return alternateTitles.stream()
      .map(title -> title.getTitleType() + " - " + title.getAlternateTitle())
      .collect(Collectors.joining(", "));
  }

  private String mapIdentifierId(List<Identifier> identifiers, TypeEnum type, SubtypeEnum subtype) {
    return identifiers.stream()
      .filter(identifier -> identifier.getType().equals(type))
      .filter(identifier -> identifier.getSubtype().equals(subtype))
      .map(Identifier::getId)
      .collect(Collectors.joining(", "));
  }

  private String mapShowToPatrons(VisibilityData visibility) {
    var result = visibility.getIsHidden() ? "No" : "Yes";
    var reason = visibility.getReason();

    if (reason != null && !reason.isBlank()) {
      result += String.format(" (%s)", reason);
    }
    return result;
  }
}
