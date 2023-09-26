package org.folio.dew.utils;

import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateToStringWithoutTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.Test;

class BulkEditProcessorHelperTest {

  @Test
  void dateUTCToStringTest() {
    Date date = new Date(1675540861000L);
    assertEquals("2023-02-04 20:01:01.000Z", dateToString(date));
  }

  @Test
  void dateUTCWithoutTimeToStringTest() {
    Date date = new Date(1675540861000L);
    assertEquals("2023-02-04", dateToStringWithoutTime(date));
  }

}
