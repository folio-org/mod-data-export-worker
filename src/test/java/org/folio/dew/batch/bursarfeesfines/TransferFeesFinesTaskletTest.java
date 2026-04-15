package org.folio.dew.batch.bursarfeesfines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

@ExtendWith(MockitoExtension.class)
class TransferFeesFinesTaskletTest {

  @Mock
  BursarExportService bursarExportService;

  @InjectMocks
  TransferFeesFinesTasklet tasklet;

  @Mock
  StepContribution mockedStepContribution;

  @Mock
  StepExecution mockedStepExecution;

  @Mock
  JobExecution mockedJobExecution;

  @Mock
  ExecutionContext mockedExecutionContext;

  @BeforeEach
  void setUp() {
    lenient().when(mockedStepContribution.getStepExecution()).thenReturn(mockedStepExecution);
    lenient().when(mockedStepExecution.getJobExecution()).thenReturn(mockedJobExecution);
    lenient().when(mockedJobExecution.getExecutionContext()).thenReturn(mockedExecutionContext);
  }

  @Test
  void testDryRun() {
    when(mockedExecutionContext.get("jobConfig", BursarExportJob.class)).thenReturn(jobConfigWithDryRun(true));

    assertEquals(RepeatStatus.FINISHED, tasklet.execute(mockedStepContribution, null));

    verifyNoInteractions(bursarExportService);
  }

  @Test
  void testNoAccounts() {
    when(mockedExecutionContext.get("jobConfig", BursarExportJob.class)).thenReturn(jobConfigWithDryRun(false));
    when(mockedExecutionContext.get("filteredAccounts")).thenReturn(List.of());

    assertEquals(RepeatStatus.FINISHED, tasklet.execute(mockedStepContribution, null));

    verifyNoInteractions(bursarExportService);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = { false })
  void testNormal(Boolean dryRun) {
    List<AccountWithAncillaryData> accounts = List.of(
      AccountWithAncillaryData.builder().build(),
      AccountWithAncillaryData.builder().build()
    );
    BursarExportJob jobConfig = jobConfigWithDryRun(dryRun);
    when(mockedExecutionContext.get("jobConfig", BursarExportJob.class)).thenReturn(jobConfig);
    when(mockedExecutionContext.get("filteredAccounts")).thenReturn(accounts);

    assertEquals(RepeatStatus.FINISHED, tasklet.execute(mockedStepContribution, null));

    verify(bursarExportService, only()).transferAccounts(accounts, jobConfigWithDryRun(dryRun));
  }

  static BursarExportJob jobConfigWithDryRun(Boolean dryRun) {
    return new BursarExportJob().dryRun(dryRun);
  }
}
