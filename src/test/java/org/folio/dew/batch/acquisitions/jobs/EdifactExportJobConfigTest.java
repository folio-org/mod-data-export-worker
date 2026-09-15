package org.folio.dew.batch.acquisitions.jobs;

import static org.folio.dew.batch.acquisitions.jobs.decider.ExportStepDecision.PROCESS;
import static org.folio.dew.batch.acquisitions.jobs.decider.ExportStepDecision.SKIP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.folio.dew.batch.acquisitions.jobs.decider.ExportStepDecision;
import org.folio.dew.config.kafka.KafkaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;

class EdifactExportJobConfigTest {

  private static final String MAP_STEP = "mapToEdifactOrdersStep";
  private static final String MINIO_STEP = "saveToMinIOStep";
  private static final String FTP_STEP = "saveToFTPStep";
  private static final String EMAIL_STEP = "sendToEmailStep";
  private static final String HISTORY_STEP = "createExportHistoryRecordsStep";

  private JobRepository jobRepository;

  @BeforeEach
  void setUp() {
    jobRepository = new ResourcelessJobRepository();
  }

  static List<Arguments> flowScenarios() {
    return List.of(
      // ftpDecision, emailDecision, historyDecision, failingStep, expectedStatus, expectedExecutedSteps
      Arguments.of(PROCESS, SKIP, PROCESS, null, BatchStatus.COMPLETED, List.of(MAP_STEP, MINIO_STEP, FTP_STEP, HISTORY_STEP)),
      Arguments.of(PROCESS, SKIP, SKIP, null, BatchStatus.COMPLETED, List.of(MAP_STEP, MINIO_STEP, FTP_STEP)),
      Arguments.of(PROCESS, SKIP, PROCESS, FTP_STEP, BatchStatus.FAILED, List.of(MAP_STEP, MINIO_STEP, FTP_STEP)),
      Arguments.of(PROCESS, SKIP, SKIP, FTP_STEP, BatchStatus.FAILED, List.of(MAP_STEP, MINIO_STEP, FTP_STEP)),
      Arguments.of(SKIP, PROCESS, PROCESS, null, BatchStatus.COMPLETED, List.of(MAP_STEP, MINIO_STEP, EMAIL_STEP, HISTORY_STEP)),
      Arguments.of(SKIP, PROCESS, SKIP, null, BatchStatus.COMPLETED, List.of(MAP_STEP, MINIO_STEP, EMAIL_STEP)),
      Arguments.of(SKIP, PROCESS, PROCESS, EMAIL_STEP, BatchStatus.FAILED, List.of(MAP_STEP, MINIO_STEP, EMAIL_STEP)),
      Arguments.of(SKIP, PROCESS, SKIP, EMAIL_STEP, BatchStatus.FAILED, List.of(MAP_STEP, MINIO_STEP, EMAIL_STEP)),
      Arguments.of(SKIP, SKIP, PROCESS, null, BatchStatus.COMPLETED, List.of(MAP_STEP, MINIO_STEP, HISTORY_STEP)),
      Arguments.of(SKIP, SKIP, SKIP, null, BatchStatus.COMPLETED, List.of(MAP_STEP, MINIO_STEP))
    );
  }

  @ParameterizedTest
  @MethodSource("flowScenarios")
  void edifactOrdersExportJob_shouldRouteStepsAndFailOnDeliveryFailure(ExportStepDecision ftpDecision,
                                                                       ExportStepDecision emailDecision,
                                                                       ExportStepDecision historyDecision,
                                                                       String failingStep,
                                                                       BatchStatus expectedStatus,
                                                                       List<String> expectedExecutedSteps) throws Exception {
    Map<String, JobExecutionDecider> deciders = Map.of(
      FTP_STEP, decider(ftpDecision),
      EMAIL_STEP, decider(emailDecision),
      HISTORY_STEP, decider(historyDecision));

    Job job = new EdifactExportJobConfig().edifactOrdersExportJob(
      new EdiExportJobCompletionListener(mock(KafkaService.class)), jobRepository,
      step(MAP_STEP, failingStep), step(MINIO_STEP, failingStep), step(FTP_STEP, failingStep),
      step(EMAIL_STEP, failingStep), step(HISTORY_STEP, failingStep), deciders);

    var jobParameters = new JobParameters();
    var jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
    var jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
    job.execute(jobExecution);

    assertEquals(expectedStatus, jobExecution.getStatus());
    assertEquals(expectedExecutedSteps, executedStepNames(jobExecution));
  }

  private Step step(String name, String failingStep) {
    return new StepBuilder(name, jobRepository)
      .tasklet((contribution, chunkContext) -> {
        if (name.equals(failingStep)) {
          throw new IllegalStateException(name + " failed");
        }
        return RepeatStatus.FINISHED;
      }, new ResourcelessTransactionManager())
      .build();
  }

  private static JobExecutionDecider decider(ExportStepDecision decision) {
    return (jobExecution, stepExecution) -> new FlowExecutionStatus(decision.getStatus());
  }

  private static List<String> executedStepNames(JobExecution jobExecution) {
    return jobExecution.getStepExecutions().stream()
      .map(StepExecution::getStepName)
      .toList();
  }

}
