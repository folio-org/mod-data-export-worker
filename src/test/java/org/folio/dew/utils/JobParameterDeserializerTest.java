package org.folio.dew.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.folio.de.entity.JobCommand;
import org.folio.dew.config.JacksonConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JobParameterDeserializerTest {

  private final ObjectMapper objectMapper = new JacksonConfiguration().get();

  @Test
  void checkCorrectJobParameterDeserialization() throws IOException, ParseException {
    var jobParamJson = getMockData("upload/jobParameter.json");

    var jobCommand = objectMapper.readValue(jobParamJson, JobCommand.class);
    var sampleDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse("2023-02-15T00:00:00.000");

    assertNotNull(jobCommand);
    assertNotNull(jobCommand.getJobParameters());
    assertEquals("((date>=\"2023-02-15T00:00:00.000\" and date<=\"2023-02-15T23:59:59.999\")) sortby date/sort.descending", jobCommand.getJobParameters().getString("String"));
    assertEquals(10.111d, jobCommand.getJobParameters().getDouble("Double"));
    assertEquals(Long.valueOf(10L), jobCommand.getJobParameters().getLong("Long"));
    assertEquals(10, Objects.requireNonNull(jobCommand.getJobParameters().getParameter("Integer")).value());
    assertEquals(Boolean.TRUE, Objects.requireNonNull(jobCommand.getJobParameters().getParameter("Boolean")).value());
    assertEquals(sampleDate, Objects.requireNonNull(jobCommand.getJobParameters().getParameter("Date")).value());
  }

}
