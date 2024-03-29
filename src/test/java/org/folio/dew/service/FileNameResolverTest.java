package org.folio.dew.service;

import static org.folio.dew.domain.dto.ExportType.AUTH_HEADINGS_UPDATES;
import static org.folio.dew.domain.dto.ExportType.E_HOLDINGS;
import static org.folio.dew.domain.dto.ExportType.FAILED_LINKED_BIB_UPDATES;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.folio.de.entity.JobCommand;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

@ExtendWith(MockitoExtension.class)
class FileNameResolverTest {
  @Mock
  private ObjectMapper objectMapper;
  @InjectMocks
  private FileNameResolver service;

  @SneakyThrows
  @MethodSource("recordTypes")
  @ParameterizedTest
  void resolve_success_eHoldings_recordTypes(EHoldingsExportConfig.RecordTypeEnum recordType, String endFileName) {
    var jobCommand = new JobCommand();
    var jobParameters = new JobParameters(Map.of("eHoldingsExportConfig", new JobParameter("any", String.class)));
    var config = new EHoldingsExportConfig()
      .recordId("test_id")
      .recordType(recordType);

    jobCommand.setExportType(E_HOLDINGS);
    jobCommand.setJobParameters(jobParameters);

    when(objectMapper.readValue(anyString(), eq(EHoldingsExportConfig.class)))
      .thenReturn(config);

    var result = service.resolve(jobCommand, "", "any_job_id");

    assertTrue(result.endsWith("test_id_" + endFileName));
  }

  @Test
  @SneakyThrows
  void resolve_success_authority_control_authority_recordTypes() {
    var jobParameters = new JobParameters(Map.of("authorityControlExportConfig", new JobParameter("any", String.class)));
    var jobCommand = new JobCommand();
    jobCommand.setExportType(AUTH_HEADINGS_UPDATES);
    jobCommand.setJobParameters(jobParameters);

    var result = service.resolve(jobCommand, "", "any_job_id");

    assertTrue(result.endsWith("auth_headings_updates.csv"));
  }

  @Test
  @SneakyThrows
  void resolve_success_authority_control_instance_recordTypes() {
    var jobParameters = new JobParameters(Map.of("authorityControlExportConfig", new JobParameter("any", String.class)));
    var jobCommand = new JobCommand();
    jobCommand.setExportType(FAILED_LINKED_BIB_UPDATES);
    jobCommand.setJobParameters(jobParameters);

    var result = service.resolve(jobCommand, "", "any_job_id");

    assertTrue(result.endsWith("failed_linked_bib_updates.csv"));
  }

  public static Stream<Arguments> recordTypes() {
    return Stream.of(
      arguments(EHoldingsExportConfig.RecordTypeEnum.PACKAGE, "package.csv"),
      arguments(EHoldingsExportConfig.RecordTypeEnum.RESOURCE, "resource.csv"));
  }
}
