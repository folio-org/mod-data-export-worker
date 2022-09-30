package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.utils.Constants.EDIFACT_EXPORT_DIR_NAME;
import static org.folio.dew.utils.Constants.getWorkingDirectory;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.PurchaseOrdersToEdifactMapper;
import org.folio.dew.repository.LocalFilesStorage;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

class EdiExportJobCompletionListenerTest extends BaseBatchTest {

  private final String WORK_DIR = getWorkingDirectory(springApplicationName, EDIFACT_EXPORT_DIR_NAME);
  private final String TEST_FILENAME = "testEdiFile.edi";

  @Autowired
  private Job edifactExportJob;
  @Autowired
  private LocalFilesStorage localFilesStorage;
  @Mock
  private PurchaseOrdersToEdifactMapper purchaseOrdersToEdifactMapper;

  @Test
  @DirtiesContext
  void checkLocalFileStorageCleanedUp() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    doReturn(RandomStringUtils.random(100)).when(purchaseOrdersToEdifactMapper).convertOrdersToEdifact(any(), any(), anyString());

    JobExecution jobExecution = testLauncher.launchJob(getJobParameters());

    assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
    assertTrue(localFilesStorage.notExists(WORK_DIR + TEST_FILENAME));
  }

  private JobParameters getJobParameters() throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();

    // prepare local file copy
    var fileContent = RandomStringUtils.random(100);
    var uploadedFilePath = localFilesStorage.write(WORK_DIR + TEST_FILENAME, fileContent.getBytes(StandardCharsets.UTF_8));
    paramsBuilder.addString("uploadedFilePath", uploadedFilePath);

    paramsBuilder.addString("edifactOrdersExport", getMockData("edifact/edifactOrdersExport.json"));
    paramsBuilder.addString("jobId", UUID.randomUUID()
      .toString());

    return paramsBuilder.toJobParameters();
  }
}