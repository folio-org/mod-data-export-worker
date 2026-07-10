package org.folio.dew.batch.eholdings;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.folio.de.entity.EHoldingsPackage;
import org.folio.dew.domain.dto.eholdings.ContentTypeEnum;
import org.folio.dew.domain.dto.eholdings.Coverage;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EmbargoPeriod;
import org.folio.dew.domain.dto.eholdings.PackageAltName;
import org.folio.dew.domain.dto.eholdings.PackageAttributes;
import org.folio.dew.domain.dto.eholdings.PackageData;
import org.folio.dew.domain.dto.eholdings.PackageVisibility;
import org.folio.dew.domain.dto.eholdings.PackageVisibility.CategoryEnum;
import org.folio.dew.domain.dto.eholdings.Proxy;
import org.folio.dew.domain.dto.eholdings.PublicationType;
import org.folio.dew.domain.dto.eholdings.ResourcesAttributes;
import org.folio.dew.domain.dto.eholdings.ResourcesData;
import org.folio.dew.domain.dto.eholdings.VisibilityData;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;

class EHoldingsToExportFormatMapperTest {

  private final EHoldingsToExportFormatMapper mapper = new EHoldingsToExportFormatMapper();
  private static final ObjectMapper objectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  void shouldMapPackageAccessToPublicWhenFreeAccess() {
    var result = mapper.convertToExportFormat(buildPackage(true, null, null, null));
    assertThat(result.getPackageAccess()).isEqualTo("Public");
  }

  @Test
  void shouldMapPackageAccessToControlledWhenNotFreeAccess() {
    var result = mapper.convertToExportFormat(buildPackage(false, null, null, null));
    assertThat(result.getPackageAccess()).isEqualTo("Controlled");
  }

  @Test
  void shouldMapAltNamesJoinedWithSemicolon() {
    var alt1 = new PackageAltName();
    alt1.setAltName("Alt 1");
    var alt2 = new PackageAltName();
    alt2.setAltName("Alt 2");
    var alt3 = new PackageAltName();
    alt3.setAltName("Alt 3");
    var alt4 = new PackageAltName();
    alt4.setAltName("Alt 4");

    var result = mapper.convertToExportFormat(buildPackage(null, List.of(alt1, alt2), List.of(alt3, alt4), null));

    assertThat(result.getCustomAlternativeNames()).isEqualTo("Alt 1; Alt 2");
    assertThat(result.getManagedAlternativeNames()).isEqualTo("Alt 3; Alt 4");
  }

  @Test
  void shouldMapVisibilityByCategoryWithReasonAppended() {
    var visibility = List.of(
      buildVisibility(CategoryEnum.PF,   true,  "Set by System"),
      buildVisibility(CategoryEnum.FTF,  false, null),
      buildVisibility(CategoryEnum.MARC, true, "because")
    );

    var result = mapper.convertToExportFormat(buildPackage(null, null, null, visibility));

    assertThat(result.getHideInPublicationFinder()).isEqualTo("Yes (Set by System)");
    assertThat(result.getHideInFullTextFinder()).isEqualTo("No");
    assertThat(result.getExcludeFromMARCExport()).isEqualTo("Yes (because)");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @SuppressWarnings("unchecked")
  void shouldReturnEmptyForNullOrEmptyInput(List<?> list) {
    var result = mapper.convertToExportFormat(
      buildPackage(null, (List<PackageAltName>) list, (List<PackageAltName>) list, (List<PackageVisibility>) list));

    assertThat(result.getManagedAlternativeNames()).isEmpty();
    assertThat(result.getCustomAlternativeNames()).isEmpty();
    assertThat(result.getPackageAccess()).isEmpty();
    assertThat(result.getHideInPublicationFinder()).isEmpty();
    assertThat(result.getHideInFullTextFinder()).isEmpty();
    assertThat(result.getExcludeFromMARCExport()).isEmpty();
  }

  @Test
  void shouldMapProxiedUrl() {
    var proxy = new Proxy();
    proxy.setId("ezproxy");
    proxy.setProxiedUrl("https://proxy.example.com");

    var result = mapper.convertToExportFormat(buildMinimalResource(proxy));

    assertThat(result.getTitleProxiedUrl()).isEqualTo("https://proxy.example.com");
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("nullProxiedUrlProxy")
  void shouldReturnEmptyProxiedUrlForNullOrMissingUrl(Proxy proxy) {
    assertThat(mapper.convertToExportFormat(buildMinimalResource(proxy)).getTitleProxiedUrl()).isEmpty();
  }

  private static Stream<Proxy> nullProxiedUrlProxy() {
    return Stream.of(new Proxy()); // proxy present but proxiedUrl is null
  }

  private EHoldingsResourceDTO buildMinimalResource(Proxy proxy) {
    var embargo = new EmbargoPeriod();
    embargo.setEmbargoValue(0);

    var visibility = new VisibilityData();
    visibility.setIsHidden(false);

    var attrs = getResourcesAttributes(proxy, embargo, visibility);

    var data = new ResourcesData();
    data.setAttributes(attrs);
    data.setIncluded(Collections.emptyList());

    return EHoldingsResourceDTO.builder()
      .resourcesData(data)
      .notes(Collections.emptyList())
      .agreements(Collections.emptyList())
      .build();
  }

  private static @NonNull ResourcesAttributes getResourcesAttributes(Proxy proxy, EmbargoPeriod embargo,
                                                                     VisibilityData visibility) {
    var attrs = new ResourcesAttributes();
    attrs.setTitleId(1);
    attrs.setIsSelected(false);
    attrs.setIsTitleCustom(false);
    attrs.setPublicationType(PublicationType.BOOK);
    attrs.setAlternateTitles(Collections.emptyList());
    attrs.setContributors(Collections.emptyList());
    attrs.setIdentifiers(Collections.emptyList());
    attrs.setManagedCoverages(Collections.emptyList());
    attrs.setCustomCoverages(Collections.emptyList());
    attrs.setManagedEmbargoPeriod(embargo);
    attrs.setCustomEmbargoPeriod(embargo);
    attrs.setVisibilityData(visibility);
    attrs.setSubjects(Collections.emptyList());
    attrs.setProxy(proxy);
    return attrs;
  }

  private PackageVisibility buildVisibility(CategoryEnum category, boolean hidden, String reason) {
    var vis = new PackageVisibility();
    vis.setCategory(category);
    vis.setHidden(hidden);
    vis.setReason(reason);
    return vis;
  }

  @SneakyThrows
  private EHoldingsPackage buildPackage(Boolean isFreeAccess,
                                        List<PackageAltName> customAltNames,
                                        List<PackageAltName> managedAltNames,
                                        List<PackageVisibility> visibility) {
    var attrs = getPackageAttributes(isFreeAccess, customAltNames, managedAltNames, visibility);

    var packageData = new PackageData();
    packageData.setId("1-22");
    packageData.setType("packages");
    packageData.setAttributes(attrs);

    var ePackage = new EPackage();
    ePackage.setData(packageData);
    ePackage.setIncluded(Collections.emptyList());

    var entity = new EHoldingsPackage();
    entity.setId("1-22");
    entity.setJobExecutionId(1L);
    entity.setEPackage(objectMapper.writeValueAsString(ePackage));
    entity.setEProvider("null");
    entity.setAgreements("[]");
    entity.setNotes("[]");
    return entity;
  }

  private static @NonNull PackageAttributes getPackageAttributes(Boolean isFreeAccess, List<PackageAltName> customAltNames,
                                                                 List<PackageAltName> managedAltNames, List<PackageVisibility> visibility) {
    var coverage = new Coverage();
    coverage.setBeginCoverage("");
    coverage.setEndCoverage("");

    var proxy = new Proxy();
    proxy.setId("<n>");

    var attrs = new PackageAttributes();
    attrs.setContentType(ContentTypeEnum.UNKNOWN);
    attrs.setCustomCoverage(coverage);
    attrs.setProxy(proxy);
    attrs.setIsSelected(false);
    attrs.setIsFreeAccess(isFreeAccess);
    attrs.setManagedAltNames(managedAltNames);
    attrs.setCustomAltNames(customAltNames);
    attrs.setVisibility(visibility);
    return attrs;
  }
}
