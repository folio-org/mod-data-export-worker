package org.folio.dew.service.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;

class HoldingsMapperTest extends BaseBatchTest {
  @Autowired
  private HoldingsMapper holdingsMapper;

  @ParameterizedTest
  @EnumSource(HoldingsMapperTestData.class)
  @SneakyThrows
  void shouldMapHoldingsRecordToHoldingsFormat(HoldingsMapperTestData testData) {
    var holdingsRecord = objectMapper.readValue(Path.of(testData.getHoldingsRecordPath()).toFile(), HoldingsRecord.class);
    var expectedHoldingsFormat = objectMapper.readValue(Path.of(testData.getHoldingsFormatPath()).toFile(), HoldingsFormat.class);
    var actualHoldingsFormat = holdingsMapper.mapToHoldingsFormat(holdingsRecord);

    assertThat(actualHoldingsFormat, equalTo(expectedHoldingsFormat));
  }

  @ParameterizedTest
  @EnumSource(HoldingsMapperTestData.class)
  @SneakyThrows
  void shouldMapHoldingsFormatToHoldingsRecord(HoldingsMapperTestData testData) {
    var holdingsFormat = objectMapper.readValue(Path.of(testData.getHoldingsFormatPath()).toFile(), HoldingsFormat.class);
    var expectedHoldingsRecord = objectMapper.readValue(Path.of(testData.getHoldingsRecordPath()).toFile(), HoldingsRecord.class);
    expectedHoldingsRecord.setEffectiveLocationId(null);
    var actualHoldingsRecord = holdingsMapper.mapToHoldingsRecord(holdingsFormat);

    assertThat(actualHoldingsRecord, equalTo(expectedHoldingsRecord));
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
