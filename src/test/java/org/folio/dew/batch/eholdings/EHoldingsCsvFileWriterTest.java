package org.folio.dew.batch.eholdings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.folio.de.entity.EHoldingsPackage;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.eholdings.EHoldingsPackageExportFormat;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceExportFormat;
import org.folio.dew.repository.EHoldingsPackageRepository;
import org.folio.dew.repository.LocalFilesStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;

@ExtendWith(MockitoExtension.class)
class EHoldingsCsvFileWriterTest {
  @Mock
  private LocalFilesStorage localFilesStorage;
  @Mock
  private EHoldingsPackageRepository packageRepository;
  @Mock
  private EHoldingsExportConfig exportConfig;
  @Mock
  private EHoldingsToExportFormatMapper mapper;
  @Mock
  private StepExecution stepExecution;
  @Mock
  private JobExecution jobExecution;
  @Mock
  private ExecutionContext executionContext;

  private EHoldingsCsvFileWriter eHoldingsCsvFileWriter;

  @SneakyThrows
  @BeforeEach
  void setUp() {
    eHoldingsCsvFileWriter =
      new EHoldingsCsvFileWriter("any", exportConfig, packageRepository, mapper, localFilesStorage);
    lenient().when(stepExecution.getJobExecutionId()).thenReturn(1L);
    lenient().when(stepExecution.getJobExecution()).thenReturn(jobExecution);
    lenient().when(jobExecution.getExecutionContext()).thenReturn(executionContext);
    lenient().when(executionContext.getInt(anyString(), anyInt())).thenReturn(100);
    lenient().when(exportConfig.getRecordId()).thenReturn("recordId");
    lenient().when(localFilesStorage.write(anyString(), any())).thenReturn("/path");
    EHoldingsPackageExportFormat exportFormat = new EHoldingsPackageExportFormat();
    exportFormat.setPackageId("packageId");
    exportFormat.setPackageName("packageName");
    lenient().when(packageRepository.findById(any())).thenReturn(Optional.of(new EHoldingsPackage()));
    lenient().when(mapper.convertToExportFormat(any(EHoldingsPackage.class))).thenReturn(exportFormat);
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("provideParameters")
  void testBeforeStep(List<String> packageFields,
                      List<String> titleFields,
                      int localFileStorageInvocations,
                      int packageRepositoryInvocations,
                      int mapperInvocations) {

    when(exportConfig.getPackageFields()).thenReturn(packageFields);
    when(exportConfig.getTitleFields()).thenReturn(titleFields);

    eHoldingsCsvFileWriter.beforeStep(stepExecution);

    verify(localFilesStorage, times(localFileStorageInvocations)).write(anyString(), any());
    verify(packageRepository, times(packageRepositoryInvocations)).findById(any());
    verify(mapper, times(mapperInvocations)).convertToExportFormat(any(EHoldingsPackage.class));
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("writeMethodParameters")
  void testWriteMethod(List<String> titleFields, int localFileStorageInvocations){
    //Given
    List<EHoldingsResourceExportFormat> items = new ArrayList<>();
    EHoldingsResourceExportFormat resourceExportFormat = new EHoldingsResourceExportFormat();
    resourceExportFormat.setTitleId("titleId");
    items.add(resourceExportFormat);
    lenient().when(exportConfig.getTitleFields()).thenReturn(titleFields);

    //When
    eHoldingsCsvFileWriter.write(new Chunk<>(items));

    //Then
    verify(localFilesStorage, times(localFileStorageInvocations)).write(anyString(), any());
  }

  private static Stream<Arguments> provideParameters() {
    return Stream.of(
      Arguments.of(List.of(), List.of("Title Id", "Title Name"), 1, 0, 0),
      Arguments.of(null, List.of("Title Id", "Title Name"), 1, 0, 0),
      Arguments.of(List.of("packageId", "packageName"), List.of(), 2, 1, 1),
      Arguments.of(List.of("packageId", "packageName"), null, 2, 1, 1),
      Arguments.of(List.of("packageId", "packageName"), List.of("Title Id", "Title Name"), 3, 1, 1)
    );
  }

  private static Stream<Arguments> writeMethodParameters() {
    return Stream.of(
      Arguments.of(List.of(), 0),
      Arguments.of(null, 0),
      Arguments.of(List.of("titleId"), 1)
    );
  }
}
