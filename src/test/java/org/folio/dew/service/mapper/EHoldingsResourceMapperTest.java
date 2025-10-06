package org.folio.dew.service.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Path;
import java.sql.Date;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.EHoldingsResource;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.eholdings.EHoldingsResourceMapper;
import org.folio.dew.client.AgreementClient;
import org.folio.dew.domain.dto.eholdings.Contributor;
import org.folio.dew.domain.dto.eholdings.Coverage;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.folio.dew.domain.dto.eholdings.EmbargoPeriod;
import org.folio.dew.domain.dto.eholdings.Identifier;
import org.folio.dew.domain.dto.eholdings.Metadata;
import org.folio.dew.domain.dto.eholdings.Note;
import org.folio.dew.domain.dto.eholdings.Proxy;
import org.folio.dew.domain.dto.eholdings.PublicationType;
import org.folio.dew.domain.dto.eholdings.ResourcesAttributes;
import org.folio.dew.domain.dto.eholdings.ResourcesData;
import org.folio.dew.domain.dto.eholdings.Subject;
import org.folio.dew.domain.dto.eholdings.Tags;
import org.folio.dew.domain.dto.eholdings.VisibilityData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

class EHoldingsResourceMapperTest extends BaseBatchTest {

  @BeforeAll
  static void beforeAll() {
    setUpTenant("diku");
  }

  @ParameterizedTest
  @EnumSource(MapperTestData.class)
  @SneakyThrows
  void shouldMapEntityToDto(MapperTestData testData) {
    var entity = objectMapper.readValue(Path.of(testData.getEntity()).toFile(), EHoldingsResource.class);
    var expectedDto = testData.getDto();
    var actualDto = EHoldingsResourceMapper.convertToDTO(entity);

    assertThat(expectedDto.getResourcesData().getAttributes(), equalTo(actualDto.getResourcesData().getAttributes()));
    assertThat(entity.getName(), equalTo(actualDto.getResourcesData().getAttributes().getName()));
    assertThat(expectedDto.getNotes(), equalTo(actualDto.getNotes()));
    assertThat(expectedDto.getAgreements(), equalTo(actualDto.getAgreements()));
  }

  @ParameterizedTest
  @EnumSource(MapperTestData.class)
  @SneakyThrows
  void shouldMapDtoToEntity(MapperTestData testData) {
    var dto = testData.getDto();
    var expectedEntity = objectMapper.readValue(Path.of(testData.getEntity()).toFile(), EHoldingsResource.class);
    expectedEntity.setJobExecutionId(null);
    var actualEntity = EHoldingsResourceMapper.convertToEntity(dto);

    assertThat(expectedEntity.getResourcesData(), equalTo(actualEntity.getResourcesData()));
    assertThat(expectedEntity.getName(), equalTo(dto.getResourcesData().getAttributes().getName()));
    assertThat(expectedEntity.getNotes(), equalTo(actualEntity.getNotes()));
    assertThat(expectedEntity.getAgreements(), equalTo(actualEntity.getAgreements()));
  }

  @AllArgsConstructor
  @Getter
  enum MapperTestData {
    RESOURCE_WITH_NOTES_AND_AGREEMENTS(dtoWithNotesAndAgreements(), "src/test/resources/mapper/resource_entity_with_notes_and_agreements.json"),
    RESOURCE_WITHOUT_NOTES_AND_AGREEMENTS(dtoWithoutNotesAndAgreements(), "src/test/resources/mapper/resource_entity_without_notes_and_agreements.json"),
    RESOURCE_WIT_LONG_TITLE_NAME(dtoWithLongTitleName(), "src/test/resources/mapper/resource_entity_with_long_title_name.json");

    final EHoldingsResourceDTO dto;
    final String entity;
  }

  private static ResourcesData getResourcesData(){
    var contributor1 = new Contributor();
    contributor1.setType("editor");
    contributor1.setContributor("Maetzener");
    var contributor2 = new Contributor();
    contributor2.setType("author");
    contributor2.setContributor("Christoph J. Scriba");
    var contributor3 = new Contributor();
    contributor3.setType("author");
    contributor3.setContributor("Peter Schreiber");

    var identifier1 = new Identifier();
    identifier1.setId("978-3-0348-0897-2");
    identifier1.setSubtype(Identifier.SubtypeEnum.PRINT);
    identifier1.setType(Identifier.TypeEnum.ISBN);
    var identifier2 = new Identifier();
    identifier2.setId("978-3-0348-0898-9");
    identifier2.setSubtype(Identifier.SubtypeEnum.PRINT);
    identifier2.setType(Identifier.TypeEnum.ISBN);
    var identifier3 = new Identifier();
    identifier3.setId("978-3-0348-0898-9");
    identifier3.setSubtype(Identifier.SubtypeEnum.ONLINE);
    identifier3.setType(Identifier.TypeEnum.ISBN);

    var subject1 = new Subject();
    subject1.setSubject("Mathematics");
    subject1.setType("General");
    var subject2 = new Subject();
    subject2.setSubject("Geometry.  Trigonometry.  Topology");
    subject2.setType("Library of Congress");
    var subject3 = new Subject();
    subject3.setSubject("Mathematics");
    subject3.setType("Medical");
    var subject4 = new Subject();
    subject4.setSubject("Mathematics");
    subject4.setType("TLI");
    var subject5 = new Subject();
    subject5.setSubject("MATHEMATICS / Geometry / General");
    subject5.setType("BISAC");
    var subject6 = new Subject();
    subject6.setSubject("MATHEMATICS / History & Philosophy");
    subject6.setType("BISAC");

    var customEmbargoPeriod = new EmbargoPeriod();
    customEmbargoPeriod.setEmbargoUnit(null);
    customEmbargoPeriod.setEmbargoValue(0);

    var managedEmbargoPeriod = new EmbargoPeriod();
    managedEmbargoPeriod.setEmbargoUnit(null);
    managedEmbargoPeriod.setEmbargoValue(0);

    var managedCoverage = new Coverage();
    managedCoverage.setBeginCoverage("1888-01-01");
    managedCoverage.setEndCoverage("");

    var visibilityData = new VisibilityData();
    visibilityData.setIsHidden(false);
    visibilityData.setReason("");

    var proxy = new Proxy();
    proxy.setId("ezproxy");
    proxy.setInherited(true);

    var tags = new Tags();
    tags.setTagList(Collections.emptyList());

    var includedAttributes = new LinkedHashMap<String, String>();
    includedAttributes.put("name", "Test");
    includedAttributes.put("credentialsId", "80898dee-449f-44dd-9c8e-37d5eb469b1d");

    var includedMetadata = new LinkedHashMap<String, String>();
    includedMetadata.put("createdDate", "2022-05-10T05:45:33.560+00:00");
    includedMetadata.put("createdByUserId", "548a5d79-f628-5a0d-a304-11d0e41ce7b7");

    var included = new LinkedHashMap<String, String>();
    included.put("id", "31ba067b-280c-49ec-a76d-ca7deacd6ff6");
    included.put("type", "accessTypes");
    included.put("attributes", includedAttributes.toString());
    included.put("creator", new LinkedHashMap<String, String>().toString());
    included.put("usageNumber", "0");
    included.put("metadata", includedMetadata.toString());

    var attributes = new ResourcesAttributes();
    attributes.setAlternateTitles(Collections.emptyList());
    attributes.setDescription(null);
    attributes.setEdition("1");
    attributes.setIsPeerReviewed(false);
    attributes.setIsTitleCustom(false);
    attributes.setPublisherName("Springer Basel AG");
    attributes.setTitleId(333);
    attributes.setContributors(List.of(contributor1, contributor2,contributor3));
    attributes.setIdentifiers(List.of(identifier1, identifier2, identifier3));
    attributes.setName("5000 Years of Geometry: Mathematics in History and Culture");
    attributes.setPublicationType(PublicationType.BOOK);
    attributes.setSubjects(List.of(subject1, subject2, subject3, subject4, subject5, subject6));
    attributes.setCoverageStatement(null);
    attributes.setCustomEmbargoPeriod(customEmbargoPeriod);
    attributes.setIsPackageCustom(true);
    attributes.setIsSelected(true);
    attributes.setTitleHasSelectedResources(true);
    attributes.setIsTokenNeeded(false);
    attributes.setLocationId(1034557733);
    attributes.setManagedEmbargoPeriod(managedEmbargoPeriod);
    attributes.setPackageId("1-22");
    attributes.setPackageName("random");
    attributes.setUrl("");
    attributes.setProviderId(1);
    attributes.setProviderName("API DEV CORPORATE CUSTOMER");
    attributes.setVisibilityData(visibilityData);
    attributes.setManagedCoverages(List.of(managedCoverage));
    attributes.setCustomCoverages(Collections.emptyList());
    attributes.setProxy(proxy);
    attributes.setTags(tags);
    attributes.setUserDefinedField1(null);
    attributes.setUserDefinedField2(null);
    attributes.setUserDefinedField3(null);
    attributes.setUserDefinedField4(null);
    attributes.setUserDefinedField5(null);

    var resourcesData = new ResourcesData();
    resourcesData.setId("1-22-333");
    resourcesData.setType(ResourcesData.TypeEnum.RESOURCES);
    resourcesData.setAttributes(attributes);
    resourcesData.setIncluded(List.of(included));
    return resourcesData;
  }

  private static EHoldingsResourceDTO dtoWithNotesAndAgreements(){
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

    return EHoldingsResourceDTO.builder()
      .resourcesData(getResourcesData())
      .agreements(List.of(agreement1))
      .notes(List.of(note1, note2))
      .build();
  }

  private static EHoldingsResourceDTO dtoWithoutNotesAndAgreements(){
    return EHoldingsResourceDTO.builder()
      .resourcesData(getResourcesData())
      .agreements(Collections.emptyList())
      .notes(Collections.emptyList())
      .build();
  }

  private static EHoldingsResourceDTO dtoWithLongTitleName(){
    var resourceData = getResourcesData();
    resourceData.getAttributes().setName(StringUtils.repeat('a', 259));
    return EHoldingsResourceDTO.builder()
      .resourcesData(resourceData)
      .agreements(Collections.emptyList())
      .notes(Collections.emptyList())
      .build();
  }
}
