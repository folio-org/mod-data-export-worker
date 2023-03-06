package org.folio.dew.service.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsNote;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.Collections;

class HoldingsMapperTest extends BaseBatchTest {
  @Autowired
  private HoldingsMapper holdingsMapper;

  @ParameterizedTest
  @EnumSource(HoldingsMapperTestData.class)
  @SneakyThrows
  void shouldMapHoldingsRecordToHoldingsFormat(HoldingsMapperTestData testData) {
    var holdingsRecord = objectMapper.readValue(Path.of(testData.getHoldingsRecordPath()).toFile(), HoldingsRecord.class);
    var expectedHoldingsFormat = objectMapper.readValue(Path.of(testData.getHoldingsFormatPath()).toFile(), HoldingsFormat.class);
    var actualHoldingsFormat = holdingsMapper.mapToHoldingsFormat(holdingsRecord, null, null, null);

    assertThat(actualHoldingsFormat, equalTo(expectedHoldingsFormat));
  }

  @ParameterizedTest
  @EnumSource(HoldingsMapperTestData.class)
  @SneakyThrows
  void shouldMapHoldingsFormatToHoldingsRecord(HoldingsMapperTestData testData) {
    var holdingsFormat = objectMapper.readValue(Path.of(testData.getHoldingsFormatPath()).toFile(), HoldingsFormat.class);
    var expectedHoldingsRecord = objectMapper.readValue(Path.of(testData.getHoldingsRecordPath()).toFile(), HoldingsRecord.class);
    var actualHoldingsRecord = holdingsMapper.mapToHoldingsRecord(holdingsFormat);

    assertThat(actualHoldingsRecord, equalTo(expectedHoldingsRecord));
  }

  @Test
  void shouldReturnInitialIdsForWrongReferenceIdsWhenMappingToHoldings() {
    var holdingsFormat = HoldingsFormat.builder()
      .id("43ce3fe9-64f1-45cc-93dc-69c9a9743c12")
      .holdingsType("9faafa84-efa7-4c81-8041-3937ff23ccd6")
      .callNumberType("4e4c7814-a3e8-45ca-9482-4b95b5b98f9c")
      .notes("865e7c84-9219-4f46-9608-2ac460c691c8;note;false")
      .illPolicy("a688ea6c-1d05-4a97-9ebb-9c28dc23ee2d")
      .source("f308c6a4-426b-48b4-a1f7-cf0a4da01826")
      .statisticalCodes("bdc3d37d-6cd9-4429-8fb8-e40c9c239278")
      .permanentLocation("Annex")
      .instance(";5bf370e0-8cca-4d9c-82e4-5170ab2a0a39")
      .build();

    var expectedHoldingsRecord = new HoldingsRecord()
      .id("43ce3fe9-64f1-45cc-93dc-69c9a9743c12")
      .holdingsTypeId("9faafa84-efa7-4c81-8041-3937ff23ccd6")
      .callNumberTypeId("4e4c7814-a3e8-45ca-9482-4b95b5b98f9c")
      .notes(Collections.singletonList(new HoldingsNote()
        .holdingsNoteTypeId("865e7c84-9219-4f46-9608-2ac460c691c8")
        .note("note")
        .staffOnly(false)))
      .illPolicyId("a688ea6c-1d05-4a97-9ebb-9c28dc23ee2d")
      .sourceId("f308c6a4-426b-48b4-a1f7-cf0a4da01826")
      .statisticalCodeIds(Collections.singletonList("bdc3d37d-6cd9-4429-8fb8-e40c9c239278"))
      .permanentLocationId("53cf956f-c1df-410b-8bea-27f712cca7c0")
      .instanceId("5bf370e0-8cca-4d9c-82e4-5170ab2a0a39");

    var actualHoldingsRecord = holdingsMapper.mapToHoldingsRecord(holdingsFormat);

    assertEquals(actualHoldingsRecord.getHoldingsTypeId(), expectedHoldingsRecord.getHoldingsTypeId());
    assertEquals(actualHoldingsRecord.getCallNumberTypeId(), expectedHoldingsRecord.getCallNumberTypeId());
    assertEquals(actualHoldingsRecord.getNotes().get(0).getHoldingsNoteType(), expectedHoldingsRecord.getNotes().get(0).getHoldingsNoteType());
    assertEquals(actualHoldingsRecord.getIllPolicyId(), expectedHoldingsRecord.getIllPolicyId());
    assertEquals(actualHoldingsRecord.getSourceId(), expectedHoldingsRecord.getSourceId());
    assertEquals(actualHoldingsRecord.getStatisticalCodeIds().get(0), expectedHoldingsRecord.getStatisticalCodeIds().get(0));
  }

  @Test
  @SneakyThrows
  void shouldIgnoreListsWithNullsAndNullObjects() {
    var holdingsRecord = objectMapper.readValue(Path.of("src/test/resources/mapper/holdings_with_nulls.json").toFile(), HoldingsRecord.class);
    var holdingsFormat = holdingsMapper.mapToHoldingsFormat(holdingsRecord, null, null, null);
    assertEquals("d6510242-5ec3-42ed-b593-3585d2e48fd6;action note;false|d6510242-5ec3-42ed-b593-3585d2e48fd6;action note;", holdingsFormat.getNotes());
    assertEquals("statement;statement note;statements staff note|statement2;statement note2;statements staff note2", holdingsFormat.getHoldingsStatements());
    assertEquals("|true;enum;chronology|;enum2;chronology2", holdingsFormat.getReceivingHistory());
  }

  @AllArgsConstructor
  @Getter
  enum HoldingsMapperTestData {
    FULL_DATA_RECORD("src/test/resources/mapper/holdings_format_full.json", "src/test/resources/mapper/holdings_record_full.json"),
    MIN_DATA_RECORD("src/test/resources/mapper/holdings_format_min.json", "src/test/resources/mapper/holdings_record_min.json");

    final String holdingsFormatPath;
    final String holdingsRecordPath;
  }
}
