package org.folio.dew.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.HoldingsContentUpdate.ActionEnum.REPLACE_WITH;
import static org.folio.dew.domain.dto.HoldingsContentUpdate.OptionEnum.TEMPORARY_LOCATION;
import static org.folio.dew.domain.dto.IdentifierType.HRID;
import static org.folio.dew.domain.dto.JobParameterNames.PREVIEW_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.IDENTIFIER_TYPE;
import static org.folio.dew.utils.Constants.PREVIEW_PREFIX;
import static org.folio.dew.utils.Constants.UPDATED_PREFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.HoldingsContentUpdate;
import org.folio.dew.domain.dto.HoldingsContentUpdateCollection;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.service.update.BulkEditHoldingsContentUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.UUID;

class BulkEditHoldingsContentUpdateServiceTest extends BaseBatchTest {
  private static final String HOLDINGS_DATA = "src/test/resources/output/bulk_edit_holdings_records_output.csv";
  private static final String HOLDINGS_DATA_MARC = "src/test/resources/output/bulk_edit_holdings_records_output_marc.csv";

  @Autowired
  private BulkEditHoldingsContentUpdateService contentUpdateService;

  @Autowired
  private BulkEditProcessingErrorsService errorsService;

  @Test
  @SneakyThrows
  void shouldCreateUpdatedAndPreviewFilesAndUpdateJobCommand() {
    var uploadedFileName = FilenameUtils.getName(HOLDINGS_DATA);
    var updatedFileName = UPDATED_PREFIX + uploadedFileName;
    var previewFileName = PREVIEW_PREFIX + uploadedFileName;
    remoteFilesStorage.upload(uploadedFileName, HOLDINGS_DATA);
    var jobId = UUID.randomUUID();
    var jobCommand = new JobCommand();
    jobCommand.setId(jobId);
    jobCommand.setExportType(BULK_EDIT_IDENTIFIERS);
    jobCommand.setJobParameters(new JobParametersBuilder()
      .addString(TEMP_OUTPUT_FILE_PATH, "test/path/" + HOLDINGS_DATA.replace(CSV_EXTENSION, EMPTY))
      .toJobParameters());

    jobCommandsReceiverService.addBulkEditJobCommand(jobCommand);

    var contentUpdates = new HoldingsContentUpdateCollection()
      .holdingsContentUpdates(Collections.singletonList(new HoldingsContentUpdate()
        .option(TEMPORARY_LOCATION)
        .action(REPLACE_WITH)
        .value("Annex")))
      .totalRecords(1);

    var res = contentUpdateService.process(jobCommand, contentUpdates);

    assertThat(res.getEntitiesForPreview(), hasSize(2));
    assertThat(res.getEntitiesForPreview().stream().allMatch(holdingsFormat -> "Annex".equals(holdingsFormat.getTemporaryLocation())), is(true));

    assertThat(remoteFilesStorage.containsFile(updatedFileName), is(true));
    assertThat(remoteFilesStorage.containsFile(previewFileName), is(true));

    assertThat(jobCommand.getExportType(), equalTo(BULK_EDIT_UPDATE));
    assertThat(jobCommand.getJobParameters().getString(UPDATED_FILE_NAME), equalTo(updatedFileName));
    assertThat(jobCommand.getJobParameters().getString(PREVIEW_FILE_NAME), equalTo(previewFileName));
  }

  @Test
  @SneakyThrows
  void shouldNotUpdateHoldingsWithSourceMARC() {
    var uploadedFileName = FilenameUtils.getName(HOLDINGS_DATA_MARC);
    remoteFilesStorage.upload(uploadedFileName, HOLDINGS_DATA_MARC);
    var jobId = UUID.randomUUID();
    var jobCommand = new JobCommand();
    jobCommand.setId(jobId);
    jobCommand.setExportType(BULK_EDIT_IDENTIFIERS);
    jobCommand.setJobParameters(new JobParametersBuilder()
      .addString(FILE_NAME, "filename.csv")
      .addString(IDENTIFIER_TYPE, HRID.getValue())
      .addString(TEMP_OUTPUT_FILE_PATH, "test/path/" + HOLDINGS_DATA_MARC.replace(CSV_EXTENSION, EMPTY))
      .toJobParameters());

    jobCommandsReceiverService.addBulkEditJobCommand(jobCommand);

    var contentUpdates = new HoldingsContentUpdateCollection()
      .holdingsContentUpdates(Collections.singletonList(new HoldingsContentUpdate()
        .option(TEMPORARY_LOCATION)
        .action(REPLACE_WITH)
        .value("Annex")))
      .totalRecords(1);

    var res = contentUpdateService.process(jobCommand, contentUpdates);
    assertThat(res.getEntitiesForPreview(), hasSize(3));
    assertThat(res.getEntitiesForPreview().get(0).getTemporaryLocation(), equalTo("Annex"));
    assertThat(res.getEntitiesForPreview().get(1).getTemporaryLocation(), equalTo("Annex"));
    assertThat(res.getEntitiesForPreview().get(2).getTemporaryLocation(), equalTo("Main Library"));

    var errors = errorsService.readErrorsFromCSV(jobId.toString(), jobCommand.getJobParameters().getString(FILE_NAME), Integer.MAX_VALUE);
    assertThat(errors.getErrors(), hasSize(2));
    assertThat(errors.getErrors().get(0).getMessage(), equalTo("ho14,No change in value needed"));
    assertThat(errors.getErrors().get(1).getMessage(), equalTo("ho15,Holdings records that have source \"MARC\" cannot be changed"));
  }
}
