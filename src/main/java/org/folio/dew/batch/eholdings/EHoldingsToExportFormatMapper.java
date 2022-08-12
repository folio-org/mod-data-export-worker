package org.folio.dew.batch.eholdings;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import static org.folio.dew.utils.Constants.DATE_TIME_PATTERN;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import joptsimple.internal.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import org.folio.dew.client.AgreementClient.Agreement;
import org.folio.dew.domain.dto.eholdings.AccessTypeData;
import org.folio.dew.domain.dto.eholdings.AlternateTitle;
import org.folio.dew.domain.dto.eholdings.Contributor;
import org.folio.dew.domain.dto.eholdings.Coverage;
import org.folio.dew.domain.dto.eholdings.EHoldingsPackage;
import org.folio.dew.domain.dto.eholdings.EHoldingsResource;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceExportFormat;
import org.folio.dew.domain.dto.eholdings.EmbargoPeriod;
import org.folio.dew.domain.dto.eholdings.Identifier;
import org.folio.dew.domain.dto.eholdings.Identifier.SubtypeEnum;
import org.folio.dew.domain.dto.eholdings.Identifier.TypeEnum;
import org.folio.dew.domain.dto.eholdings.Note;
import org.folio.dew.domain.dto.eholdings.Proxy;
import org.folio.dew.domain.dto.eholdings.Subject;
import org.folio.dew.domain.dto.eholdings.Tags;
import org.folio.dew.domain.dto.eholdings.VisibilityData;

@Component
public class EHoldingsToExportFormatMapper {

  private static final String ACCESS_TYPE_INCLUDED = "accessTypes";
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
  public static final String PIPE_DELIMITER = " | ";
  private final ObjectMapper objectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public List<EHoldingsResourceExportFormat> convertToExportFormat(EHoldingsPackage eHoldingsPackage, List<EHoldingsResource> data) {
    if (data.isEmpty()) return singletonList(convertToExportFormat(eHoldingsPackage));

    return data.stream()
      .map(resource -> convertToExportFormat(eHoldingsPackage, resource))
      .collect(Collectors.toList());
  }

  public EHoldingsResourceExportFormat convertToExportFormat(EHoldingsPackage eHoldingsPackage) {
    var exportFormat = new EHoldingsResourceExportFormat();
    mapPackageToExportFormat(exportFormat, eHoldingsPackage);
    return exportFormat;
  }

  public EHoldingsResourceExportFormat convertToExportFormat(EHoldingsPackage eHoldingsPackage, EHoldingsResource eHoldingsResource) {
    var exportFormat = new EHoldingsResourceExportFormat();
    mapPackageToExportFormat(exportFormat, eHoldingsPackage);
    mapResourceDataToExportFormat(exportFormat, eHoldingsResource);
    return exportFormat;
  }

  public List<String> convertNotes(List<Note> notes) {
    return notes == null ? emptyList() : notes.stream()
      .map(this::noteToString)
      .collect(Collectors.toList());
  }

  public String convertAgreements(List<Agreement> agreements) {
    return agreements == null ? EMPTY : agreements.stream()
      .map(this::agreementToString)
      .collect(Collectors.joining(PIPE_DELIMITER));
  }

  @NotNull
  private String agreementToString(Agreement agreement) {
    return Strings.join(new String[] {agreement.getStartDate(), agreement.getName(), agreement.getStatus()}, ";");
  }

  private String noteToString(Note note) {
    var pieces = new String[] {
      dateToString(note.getMetadata().getUpdatedDate()),
      note.getType(),
      note.getTitle(),
      note.getContent()
    };
    return Strings.join(pieces, ";");
  }

  private String dateToString(Date date) {
    return nonNull(date) ? OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC).format(DATE_FORMAT) : EMPTY;
  }

  private void mapPackageToExportFormat(EHoldingsResourceExportFormat exportFormat, EHoldingsPackage eHoldingsPackage) {
    var ePackage = eHoldingsPackage.getEPackage();
    var packageAtr = ePackage.getData().getAttributes();

    exportFormat.setProviderId(packageAtr.getProviderId().toString());
    exportFormat.setProviderName(packageAtr.getProviderName());
    exportFormat.setPackageId(ePackage.getData().getId());
    exportFormat.setPackageName(packageAtr.getName());
    exportFormat.setPackageType(packageAtr.getPackageType());
    exportFormat.setPackageContentType(packageAtr.getContentType().getValue());
    exportFormat.setPackageCustomCoverage(mapCoverage(packageAtr.getCustomCoverage()));
    exportFormat.setPackageProxy(mapProxy(packageAtr.getProxy()));
    exportFormat.setPackageTags(mapTags(packageAtr.getTags()));
    exportFormat.setPackageHoldingsStatus(mapHoldingsStatus(packageAtr.getIsSelected()));
    exportFormat.setPackageShowToPatrons(mapShowToPatrons(packageAtr.getVisibilityData()));
    exportFormat.setPackageAutomaticallySelect(convertBoolToStr(packageAtr.getAllowKbToAddTitles()));
    exportFormat.setPackageAccessStatusType(mapAccessType(ePackage.getIncluded()));
    exportFormat.setPackageNotes(convertNotes(eHoldingsPackage.getNotes()));
    exportFormat.setPackageAgreements(convertAgreements(eHoldingsPackage.getAgreements()));
  }

  private void mapResourceDataToExportFormat(EHoldingsResourceExportFormat exportFormat, EHoldingsResource eHoldingsResource) {
    var data = eHoldingsResource.getResourcesData();
    var resourceAtr = data.getAttributes();

    exportFormat.setTitleId(resourceAtr.getTitleId().toString());
    exportFormat.setTitleName(resourceAtr.getName());
    exportFormat.setAlternateTitles(mapAlternateTitles(resourceAtr.getAlternateTitles()));
    exportFormat.setTitleHoldingsStatus(mapHoldingsStatus(resourceAtr.getIsSelected()));
    exportFormat.setPublicationType(resourceAtr.getPublicationType().getValue());
    exportFormat.setTitleType(mapTitleType(resourceAtr.getIsTitleCustom()));
    exportFormat.setContributors(mapContributors(resourceAtr.getContributors()));
    exportFormat.setEdition(resourceAtr.getEdition());
    exportFormat.setPublisher(resourceAtr.getPublisherName());
    exportFormat.setPeerReviewed(convertBoolToStr(resourceAtr.getIsPeerReviewed()));
    exportFormat.setDescription(resourceAtr.getDescription());
    exportFormat.setManagedCoverage(mapCoverage(resourceAtr.getManagedCoverages()));
    exportFormat.setCustomCoverage(mapCoverage(resourceAtr.getManagedCoverages()));
    exportFormat.setCoverageStatement(resourceAtr.getCoverageStatement());
    exportFormat.setManagedEmbargo(mapEmbargo(resourceAtr.getManagedEmbargoPeriod()));
    exportFormat.setCustomEmbargo(mapEmbargo(resourceAtr.getCustomEmbargoPeriod()));
    exportFormat.setTitleShowToPatrons(mapShowToPatrons(resourceAtr.getVisibilityData()));
    exportFormat.setTitleProxy(mapProxy(resourceAtr.getProxy()));
    exportFormat.setUrl(resourceAtr.getUrl());
    exportFormat.setSubjects(mapSubjects(resourceAtr.getSubjects()));
    exportFormat.setCustomValue1(resourceAtr.getUserDefinedField1());
    exportFormat.setCustomValue2(resourceAtr.getUserDefinedField2());
    exportFormat.setCustomValue3(resourceAtr.getUserDefinedField3());
    exportFormat.setCustomValue4(resourceAtr.getUserDefinedField4());
    exportFormat.setCustomValue5(resourceAtr.getUserDefinedField5());
    exportFormat.setTitleAccessStatusType(mapAccessType(data.getIncluded()));
    exportFormat.setTitleTags(mapTags(resourceAtr.getTags()));
    exportFormat.setISBNPrint(
      mapIdentifierId(resourceAtr.getIdentifiers(), TypeEnum.ISBN, SubtypeEnum.PRINT));
    exportFormat.setISBNOnline(
      mapIdentifierId(resourceAtr.getIdentifiers(), TypeEnum.ISBN, SubtypeEnum.ONLINE));
    exportFormat.setISSNPrint(
      mapIdentifierId(resourceAtr.getIdentifiers(), TypeEnum.ISSN, SubtypeEnum.PRINT));
    exportFormat.setISSNOnline(
      mapIdentifierId(resourceAtr.getIdentifiers(), TypeEnum.ISSN, SubtypeEnum.ONLINE));
    exportFormat.setTitleNotes(convertNotes(eHoldingsResource.getNotes()));
    exportFormat.setTitleAgreements(convertAgreements(eHoldingsResource.getAgreements()));
  }

  private Object getIncludedObject(List<Object> included, String type) {
    if (isNull(included)) { return null; }
    return included.stream()
      .map(LinkedHashMap.class::cast)
      .filter(o -> o.get("type").equals(type))
      .findFirst().orElse(null);
  }

  private String convertBoolToStr(Boolean isTrue) {
    if (isNull(isTrue)) { return ""; }
    return Boolean.TRUE.equals(isTrue) ? "Yes" : "No";
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
    if (isNull(tags)) { return ""; }
    return String.join(PIPE_DELIMITER, tags.getTagList());
  }

  private String mapEmbargo(EmbargoPeriod embargo) {
    if (embargo.getEmbargoValue() <= 0) { return ""; }
    return embargo.getEmbargoValue() + " " + embargo.getEmbargoUnit();
  }

  private String mapProxy(Proxy proxy) {
    if (proxy.getId().equals("<n>")) {
      return "None";
    }
    var inherited = Boolean.TRUE.equals(proxy.getInherited()) ? "(inherited)" : "";
    return proxy.getId() + inherited;
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

  private String mapContributors(List<Contributor> contributors) {
    return contributors.stream()
      .map(contributor -> contributor.getContributor() + " (" + contributor.getType() + ')')
      .collect(Collectors.joining(PIPE_DELIMITER));
  }

  private String mapSubjects(List<Subject> subjects) {
    return subjects.stream()
      .map(Subject::getSubject)
      .collect(Collectors.joining(PIPE_DELIMITER));
  }

  private String mapCoverage(List<Coverage> coverages) {
    return coverages.stream()
      .map(this::mapCoverage)
      .collect(Collectors.joining(PIPE_DELIMITER));
  }

  private String mapAlternateTitles(List<AlternateTitle> alternateTitles) {
    return alternateTitles.stream()
      .map(title -> title.getTitleType() + " - " + title.getAlternateTitle())
      .collect(Collectors.joining(PIPE_DELIMITER));
  }

  private String mapIdentifierId(List<Identifier> identifiers, TypeEnum type, SubtypeEnum subtype) {
    return identifiers.stream()
      .filter(identifier -> identifier.getType().equals(type))
      .filter(identifier -> identifier.getSubtype().equals(subtype))
      .map(Identifier::getId)
      .collect(Collectors.joining(PIPE_DELIMITER));
  }

  private String mapShowToPatrons(VisibilityData visibility) {
    var result = Boolean.TRUE.equals(visibility.getIsHidden()) ? "No" : "Yes";
    var reason = visibility.getReason();

    if (reason != null && !reason.isBlank()) {
      result += String.format(" (%s)", reason);
    }
    return result;
  }
}
