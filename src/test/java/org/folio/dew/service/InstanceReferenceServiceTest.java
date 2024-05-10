package org.folio.dew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dew.client.InstanceNoteTypesClient;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InstanceReferenceServiceTest {
  @Mock
  private InstanceNoteTypesClient instanceNoteTypesClient;
  @Mock
  private BulkEditProcessingErrorsService errorsService;
  @InjectMocks
  private InstanceReferenceService instanceReferenceService;

  @Test
  void shouldSaveErrorWhenInstanceNoteTypeNotFound() {
    when(instanceNoteTypesClient.getNoteTypeById(anyString()))
      .thenThrow(new NotFoundException("not found"));

    instanceReferenceService.getInstanceNoteTypeNameById(UUID.randomUUID().toString(),
      new ErrorServiceArgs("jobId", "identifier", "errorFile"));

    verify(errorsService).saveErrorInCSV(eq("jobId"), eq("identifier"), any(BulkEditException.class), eq("errorFile"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldReturnEmptyNameWhenInstanceNoteTypeIsNullOrEmpty(String id) {
    var name = instanceReferenceService.getInstanceTypeNameById(id,
      new ErrorServiceArgs("jobId", "identifier", "errorFile"));
    assertThat(name).isEmpty();
  }
}
