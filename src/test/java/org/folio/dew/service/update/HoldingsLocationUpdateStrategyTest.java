package org.folio.dew.service.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class HoldingsLocationUpdateStrategyTest {
  private final HoldingsLocationUpdateStrategy updateStrategy = new HoldingsLocationUpdateStrategy();

  @ParameterizedTest
  @EnumSource(HoldingsLocationsTestData.class)
  void shouldClearTemporaryLocationAndUpdateEffectiveLocation(HoldingsLocationsTestData testData) {
    var holdingsFormat = testData.getHoldingsFormat();
    var update = testData.getUpdate();

    var updatedHoldingsFormat = updateStrategy.applyUpdate(holdingsFormat, update);

    assertThat(updatedHoldingsFormat, equalTo(testData.getExpectedHoldingsFormat()));
  }
}
