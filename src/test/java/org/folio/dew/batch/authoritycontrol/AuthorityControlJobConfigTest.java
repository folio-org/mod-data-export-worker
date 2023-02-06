package org.folio.dew.batch.authoritycontrol;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.dew.domain.dto.AuthorityControlExportConfig;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorityControlJobConfigTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AuthorityControlJobConfig jobConfig = new AuthorityControlJobConfig(objectMapper);

  @Test
  @SneakyThrows
  void shouldConvertExportConfig() {
    Date expectedFrom = new Date(2000);
    Date expectedTo = new Date();

    String exportConfig = objectMapper.writeValueAsString(buildExportConfig(expectedFrom, expectedTo));

    var result = jobConfig.exportConfig(exportConfig);
    assertEquals(expectedFrom, result.getFromDate());
    assertEquals(expectedTo, result.getToDate());
  }

  private AuthorityControlExportConfig buildExportConfig(Date fromDate, Date toDate) {
    var authorityControlJobConfig = new AuthorityControlExportConfig();
    authorityControlJobConfig.setFromDate(fromDate);
    authorityControlJobConfig.setToDate(toDate);
    return authorityControlJobConfig;
  }
}
