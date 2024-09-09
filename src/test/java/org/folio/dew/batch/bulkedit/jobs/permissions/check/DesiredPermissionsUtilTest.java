package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DesiredPermissionsUtilTest {

  @Test
  void testConvertPermissionsToList() {
    var permissions = "[\"desired-permission\",\"desired-permission-2\"]";
    var expected = List.of("desired-permission", "desired-permission-2");
    var actual = DesiredPermissionsUtil.convertPermissionsToList(permissions);

    assertEquals(expected, actual);
  }
}
