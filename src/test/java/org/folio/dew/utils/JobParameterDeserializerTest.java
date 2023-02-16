package org.folio.dew.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.de.entity.JobCommand;
import org.folio.dew.BaseBatchTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JobParameterDeserializerTest extends BaseBatchTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void checkCorrectJobParameterDeserialization() throws IOException {
    var jobParamJson = getMockData("upload/jobParameter.json");

    var jobCommand = objectMapper.readValue(jobParamJson, JobCommand.class);

    assertNotNull(jobCommand);
    assertNotNull(jobCommand.getJobParameters());
    assertNotNull(jobCommand.getJobParameters().getString("query"));
  }

}
