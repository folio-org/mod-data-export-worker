package org.folio.dew.service.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Path;
import java.sql.Date;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.folio.de.entity.EHoldingsPackage;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.eholdings.EHoldingsPackageMapper;
import org.folio.dew.client.AgreementClient;
import org.folio.dew.domain.dto.eholdings.ContentTypeEnum;
import org.folio.dew.domain.dto.eholdings.Coverage;
import org.folio.dew.domain.dto.eholdings.EHoldingsPackageDTO;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EProvider;
import org.folio.dew.domain.dto.eholdings.Metadata;
import org.folio.dew.domain.dto.eholdings.Note;
import org.folio.dew.domain.dto.eholdings.PackageAttributes;
import org.folio.dew.domain.dto.eholdings.PackageData;
import org.folio.dew.domain.dto.eholdings.ProviderAttributes;
import org.folio.dew.domain.dto.eholdings.ProviderData;
import org.folio.dew.domain.dto.eholdings.Proxy;
import org.folio.dew.domain.dto.eholdings.Tags;
import org.folio.dew.domain.dto.eholdings.Token;
import org.folio.dew.domain.dto.eholdings.VisibilityData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

class EHoldingsPackageMapperTest extends BaseBatchTest {

  @BeforeAll
  static void beforeAll() {
    setUpTenant("diku");
  }

  @ParameterizedTest
  @EnumSource(MapperTestData.class)
  @SneakyThrows
  void shouldMapEntityToDto(MapperTestData testData) {
    var entity = objectMapper.readValue(Path.of(testData.getEntity()).toFile(), EHoldingsPackage.class);
    var expectedDto = testData.getDto();
    var actualDto = EHoldingsPackageMapper.convertToDTO(entity);

    assertThat(expectedDto.getEPackage().getData(), equalTo(actualDto.getEPackage().getData()));
    assertThat(expectedDto.getEProvider().getData(), equalTo(actualDto.getEProvider().getData()));
    assertThat(expectedDto.getNotes(), equalTo(actualDto.getNotes()));
    assertThat(expectedDto.getAgreements(), equalTo(actualDto.getAgreements()));
  }

  @ParameterizedTest
  @EnumSource(MapperTestData.class)
  @SneakyThrows
  void shouldMapDtoToEntity(MapperTestData testData) {
    var dto = testData.getDto();
    var expectedEntity = objectMapper.readValue(Path.of(testData.getEntity()).toFile(), EHoldingsPackage.class);
    expectedEntity.setJobExecutionId(null);
    var actualEntity = EHoldingsPackageMapper.convertToEntity(dto);

    assertThat(expectedEntity.getEPackage(), equalTo(actualEntity.getEPackage()));
    assertThat(expectedEntity.getEProvider(), equalTo(actualEntity.getEProvider()));
    assertThat(expectedEntity.getAgreements(), equalTo(actualEntity.getAgreements()));
    assertThat(expectedEntity.getNotes(), equalTo(actualEntity.getNotes()));
  }

  @AllArgsConstructor
  @Getter
  enum MapperTestData {
    PACKAGE_WITH_NOTES_AND_AGREEMENTS(dtoWithNotesAnsAgreements(), "src/test/resources/mapper/package_entity_with_notes_and_agreements.json"),
    PACKAGE_WITHOUT_NOTES_AND_AGREEMENTS(dtoWithoutNotesAnsAgreements(), "src/test/resources/mapper/package_entity_without_notes_and_agreements.json");

    final EHoldingsPackageDTO dto;
    final String entity;
  }

  private static EPackage getEPackage(){
    var includedAttributes = new LinkedHashMap<String, String>();
    includedAttributes.put("name", "Test");
    includedAttributes.put("credentialsId", "80898dee-449f-44dd-9c8e-37d5eb469b1d");

    var includedMetadata = new LinkedHashMap<String, String>();
    includedMetadata.put("createdDate",	"2022-05-10T05:45:33.560+00:00");
    includedMetadata.put("createdByUserId",	"548a5d79-f628-5a0d-a304-11d0e41ce7b7");

    var included = new LinkedHashMap<String, String>();
    included.put("id", "31ba067b-280c-49ec-a76d-ca7deacd6ff6");
    included.put("type", "accessTypes");
    included.put("attributes", includedAttributes.toString());
    included.put("creator", new LinkedHashMap<String, String>().toString());
    included.put("usageNumber", "0");
    included.put("metadata", includedMetadata.toString());

    var customCoverage = new Coverage();
    customCoverage.setBeginCoverage("");
    customCoverage.setEndCoverage("");

    var visibilityData = new VisibilityData();
    visibilityData.isHidden(false);
    visibilityData.setReason("");

    var packageToken = new Token();
    packageToken.setFactName("[[proxyid]]");
    packageToken.setPrompt("ISBN");
    packageToken.setHelpText("<div></div>");
    packageToken.setValue("token 1-22");

    var proxy = new Proxy();
    proxy.setId("ezproxy");
    proxy.setInherited(true);

    var tags = new Tags();
    tags.setTagList(Collections.emptyList());

    var packageAttributes = new PackageAttributes();
    packageAttributes.setContentType(ContentTypeEnum.UNKNOWN);
    packageAttributes.setCustomCoverage(customCoverage);
    packageAttributes.setIsCustom(true);
    packageAttributes.setIsSelected(true);
    packageAttributes.setName("custom-package, 13");
    packageAttributes.setPackageId("22");
    packageAttributes.setPackageType("Custom");
    packageAttributes.setProviderId("1");
    packageAttributes.setProviderName("API DEV CORPORATE CUSTOMER");
    packageAttributes.setSelectedCount(2);
    packageAttributes.setTitleCount(2);
    packageAttributes.setVisibilityData(visibilityData);
    packageAttributes.setAllowKbToAddTitles(false);
    packageAttributes.setPackageToken(packageToken);
    packageAttributes.setProxy(proxy);
    packageAttributes.setTags(tags);

    var packageData = new PackageData();
    packageData.setId("1-22");
    packageData.setType("packages");
    packageData.setAttributes(packageAttributes);

    var ePackage = new EPackage();
    ePackage.setData(packageData);
    ePackage.setIncluded(List.of(included));
    return ePackage;
  }

  private static EProvider getEProvider(){
    var providerToken = new Token();
    providerToken.setFactName("[[vleid]]");
    providerToken.setPrompt("id=");
    providerToken.setHelpText("<p></p>");
    providerToken.setValue("ProviderToken 1");

    var proxy = new Proxy();
    proxy.setId("ezproxy");
    proxy.setInherited(true);

    var tags = new Tags();
    tags.setTagList(Collections.emptyList());

    var providerAttributes = new ProviderAttributes();
    providerAttributes.setName("API DEV CORPORATE CUSTOMER");
    providerAttributes.setPackagesTotal(1);
    providerAttributes.setPackagesSelected(1);
    providerAttributes.setProviderToken(providerToken);
    providerAttributes.setSupportsCustomPackages(false);
    providerAttributes.setProxy(proxy);
    providerAttributes.setTags(tags);

    var providerData = new ProviderData();
    providerData.setId("1");
    providerData.setType("providers");
    providerData.setAttributes(providerAttributes);

    var eProvider = new EProvider();
    eProvider.setData(providerData);
    eProvider.setIncluded(Collections.emptyList());
    return eProvider;
  }
  private static EHoldingsPackageDTO dtoWithNotesAnsAgreements(){
    var agreement1 = new AgreementClient.Agreement();
    agreement1.setStatus("Active");
    agreement1.setName("Test");
    agreement1.setStartDate("2022-06-01");

    var note1Metadata = new Metadata();
    note1Metadata.setCreatedDate(new Date(1617235321051L));
    note1Metadata.setCreatedByUserId(UUID.fromString("cc47a7f8-b665-4311-8401-d5bb055dc388"));
    note1Metadata.setCreatedByUsername(null);
    note1Metadata.setUpdatedDate(new Date(1617235321051L));
    note1Metadata.setUpdatedByUserId(UUID.fromString("cc47a7f8-b665-4311-8401-d5bb055dc388"));
    note1Metadata.setUpdatedByUsername(null);

    var note1 = new Note();
    note1.setId("9e235fb9-e4f8-4995-89a5-3b2fa06dfbb9");
    note1.setTypeId("5bea5cf6-d15f-4bc2-b9e1-f1631a14ab6a");
    note1.setType("Technical note");
    note1.setTitle("OCLC Record Status");
    note1.setDomain("eholdings");
    note1.setContent("<p>Not updated</p>");
    note1.setPopUpOnCheckOut(false);
    note1.setPopUpOnUser(false);
    note1.setMetadata(note1Metadata);

    var note2Metadata = new Metadata();
    note2Metadata.setCreatedDate(new Date(1585180987582L));
    note2Metadata.setCreatedByUserId(UUID.fromString("9eb67301-6f6e-468f-9b1a-6134dc39a684"));
    note2Metadata.setCreatedByUsername(null);
    note2Metadata.setUpdatedDate(new Date(1585180987582L));
    note2Metadata.setUpdatedByUserId(UUID.fromString("9eb67301-6f6e-468f-9b1a-6134dc39a684"));
    note2Metadata.setUpdatedByUsername(null);

    var note2 = new Note();
    note2.setId("0bab2772-e2c1-4a33-803c-407fb154d775");
    note2.setTypeId("25e17bd3-4c7f-42e5-9e04-65f5ea76c151");
    note2.setType("Customer Support ");
    note2.setTitle("UBM");
    note2.setDomain("eholdings");
    note2.setContent("<p>UBM</p>");
    note2.setPopUpOnCheckOut(false);
    note2.setPopUpOnUser(false);
    note2.setMetadata(note2Metadata);

    return EHoldingsPackageDTO.builder()
      .ePackage(getEPackage())
      .eProvider(getEProvider())
      .agreements(List.of(agreement1))
      .notes(List.of(note1, note2))
      .build();
  }

  private static EHoldingsPackageDTO dtoWithoutNotesAnsAgreements(){
    return EHoldingsPackageDTO.builder()
      .ePackage(getEPackage())
      .eProvider(getEProvider())
      .agreements(Collections.emptyList())
      .notes(Collections.emptyList())
      .build();
  }
}
