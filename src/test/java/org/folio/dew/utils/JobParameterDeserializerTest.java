package org.folio.dew.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.folio.de.entity.JobCommand;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.config.JacksonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JobParameterDeserializerTest {

  private ObjectMapper objectMapper = new JacksonConfiguration().get();

  @Test
  void checkCorrectJobParameterDeserialization() throws IOException, ParseException {
    var jobParamJson = getMockData("upload/jobParameter.json");

    var jobCommand = objectMapper.readValue(jobParamJson, JobCommand.class);
    var sampleDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse("2023-02-15T00:00:00.000");

    assertNotNull(jobCommand);
    assertNotNull(jobCommand.getJobParameters());
    assertEquals("((date>=\"2023-02-15T00:00:00.000\" and date<=\"2023-02-15T23:59:59.999\")) sortby date/sort.descending", jobCommand.getJobParameters().getString("stringParam"));
    assertEquals(10.111d, jobCommand.getJobParameters().getDouble("doubleParam"));
    assertEquals(Long.valueOf(10L), jobCommand.getJobParameters().getLong("longParam"));
    assertEquals(10, jobCommand.getJobParameters().getParameter("intParam").getValue());
    assertEquals(Boolean.TRUE, jobCommand.getJobParameters().getParameter("booleanParam").getValue());
    assertEquals(sampleDate, jobCommand.getJobParameters().getParameter("dateParam").getValue());
  }

}
