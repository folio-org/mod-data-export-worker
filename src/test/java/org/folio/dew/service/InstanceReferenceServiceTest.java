package org.folio.dew.service;


import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.error.NotFoundException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InstanceReferenceServiceTest extends BaseBatchTest {

  @Autowired
  private BulkEditProcessingErrorsService errorsService;
  @Autowired
  private InstanceReferenceService instanceReferenceService;

  @Test
  void shouldSaveErrorWhenInstanceNoteTypeNotFound() {
    when(instanceNoteTypesClient.getNoteTypeById(anyString()))
      .thenThrow(new NotFoundException("not found"));

    var jobId = UUID.randomUUID().toString();
    var id = UUID.randomUUID().toString();

    instanceReferenceService.getInstanceNoteTypeNameById(id,
      new ErrorServiceArgs(jobId, "identifier", "errorFile"));

    var errors = errorsService.readErrorsFromCSV(jobId, "errorFile", Integer.MAX_VALUE);

    assertThat(errors.getErrors(), Matchers.hasSize(1));
    assertThat(errors.getErrors().get(0).getMessage(), Matchers.equalTo(format("identifier,Instance note type was not found by id: [%s]", id)));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldReturnEmptyNameWhenInstanceNoteTypeIsNullOrEmpty(String id) {
    var name = instanceReferenceService.getInstanceTypeNameById(id,
      new ErrorServiceArgs("jobId", "identifier", "errorFile"));
    assertThat(name, Matchers.emptyOrNullString());
  }

  @Test
  void shouldSaveErrorWhenStatisticalCodeNotFound() {
    var jobId = UUID.randomUUID().toString();

    instanceReferenceService.getStatisticalCodeNameById(UUID.randomUUID().toString(),
      new ErrorServiceArgs(jobId, "identifier", "errorFile"));

    var errors = errorsService.readErrorsFromCSV(jobId, "errorFile", Integer.MAX_VALUE);

    assertThat(errors.getErrors(), Matchers.hasSize(1));
  }

  @Test
  void shouldSaveErrorWhenStatisticalCodeCodeNotFound() {
    var jobId = UUID.randomUUID().toString();

    instanceReferenceService.getStatisticalCodeCodeById(UUID.randomUUID().toString(),
      new ErrorServiceArgs(jobId, "identifier", "errorFile"));

    var errors = errorsService.readErrorsFromCSV(jobId, "errorFile", Integer.MAX_VALUE);

    assertThat(errors.getErrors(), Matchers.hasSize(1));
  }

  @Test
  void shouldSaveErrorWhenStatisticalCodeTypeNameNotFound() {
    var jobId = UUID.randomUUID().toString();

    instanceReferenceService.getStatisticalCodeTypeNameById(UUID.randomUUID().toString(),
      new ErrorServiceArgs(jobId, "identifier", "errorFile"));

    var errors = errorsService.readErrorsFromCSV(jobId, "errorFile", Integer.MAX_VALUE);

    assertThat(errors.getErrors(), Matchers.hasSize(1));
  }
}
