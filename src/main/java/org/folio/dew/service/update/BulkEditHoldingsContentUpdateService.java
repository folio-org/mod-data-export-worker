package org.folio.dew.service.update;

import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.PREVIEW_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.COMMA;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.IDENTIFIER_TYPE;
import static org.folio.dew.utils.Constants.NO_CHANGE_MESSAGE;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.PREVIEW_PREFIX;
import static org.folio.dew.utils.Constants.UPDATED_PREFIX;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.domain.dto.HoldingsContentUpdate;
import org.folio.dew.domain.dto.HoldingsContentUpdateCollection;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.FileOperationException;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.ContentUpdateRecords;
import org.folio.dew.service.UpdatesResult;
import org.folio.dew.service.validation.HoldingsContentUpdateValidatorService;
import org.folio.dew.utils.CsvHelper;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Log4j2
public class BulkEditHoldingsContentUpdateService {
  private final RemoteFilesStorage remoteFilesStorage;
  private final BulkEditProcessingErrorsService errorsService;
  private final HoldingsLocationUpdateStrategy locationUpdateStrategy;
  private final HoldingsContentUpdateValidatorService validatorService;

  public UpdatesResult<HoldingsFormat> process(JobCommand jobCommand, HoldingsContentUpdateCollection contentUpdates) {
    validatorService.validateContentUpdateCollection(contentUpdates);
    try {
      var fileName = FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION;
      var updatedFileName = jobCommand.getId() + PATH_SEPARATOR + UPDATED_PREFIX + fileName;
      var previewFileName = jobCommand.getId() + PATH_SEPARATOR + PREVIEW_PREFIX + fileName;
      var holdingsFormats = CsvHelper.readRecordsFromStorage(remoteFilesStorage, jobCommand.getId() + PATH_SEPARATOR + fileName, HoldingsFormat.class, true);
      var updatedHoldings = applyContentUpdates(holdingsFormats, contentUpdates, jobCommand);
      CsvHelper.saveRecordsToStorage(remoteFilesStorage, updatedHoldings.getUpdated(), HoldingsFormat.class, updatedFileName);
      CsvHelper.saveRecordsToStorage(remoteFilesStorage, updatedHoldings.getPreview(), HoldingsFormat.class, previewFileName);
      jobCommand.setJobParameters(new JobParametersBuilder(jobCommand.getJobParameters())
        .addString(UPDATED_FILE_NAME, updatedFileName)
        .addString(PREVIEW_FILE_NAME, previewFileName)
        .toJobParameters());
      jobCommand.setExportType(BULK_EDIT_UPDATE);
      return new UpdatesResult<HoldingsFormat>().withTotal(holdingsFormats.size()).withEntitiesForPreview(updatedHoldings.getPreview());
    } catch (Exception e) {
      var msg = String.format("I/O exception for job id %s, reason: %s", jobCommand.getId(), e.getMessage());
      log.error(msg);
      throw new FileOperationException(msg);
    }
  }

  private ContentUpdateRecords<HoldingsFormat> applyContentUpdates(List<HoldingsFormat> holdingsFormats, HoldingsContentUpdateCollection contentUpdateCollection, JobCommand jobCommand) {
    var updateResult = new ContentUpdateRecords<HoldingsFormat>();
    var errorStringBuilder = new StringBuilder();
    holdingsFormats.forEach(holdingsFormat -> {
      var updatedHoldingsRecord = holdingsFormat;
      if ("MARC".equals(holdingsFormat.getSource())) {
        errorStringBuilder
          .append(holdingsFormat.getIdentifier(jobCommand.getJobParameters().getString(IDENTIFIER_TYPE)))
          .append(COMMA)
          .append("Holdings records that have source \"MARC\" cannot be changed")
          .append(System.lineSeparator());
      } else {
        for (HoldingsContentUpdate contentUpdate: contentUpdateCollection.getHoldingsContentUpdates()) {
          updatedHoldingsRecord = resolveUpdateStrategy(contentUpdate).applyUpdate(updatedHoldingsRecord, contentUpdate);
        }
        if (!Objects.equals(updatedHoldingsRecord, holdingsFormat)) {
          updateResult.addToUpdated(updatedHoldingsRecord);
        } else {
          errorStringBuilder
            .append(holdingsFormat.getIdentifier(jobCommand.getJobParameters().getString(IDENTIFIER_TYPE)))
            .append(COMMA)
            .append(NO_CHANGE_MESSAGE)
            .append(System.lineSeparator());
        }
      }
      updateResult.addToPreview(updatedHoldingsRecord);
    });
    if (!errorStringBuilder.toString().isEmpty()) {
      errorsService.saveErrorInCSV(jobCommand.getId().toString(), errorStringBuilder.toString(), FilenameUtils.getName(jobCommand.getJobParameters().getString(FILE_NAME)));
    }
    return updateResult;
  }

  private UpdateStrategy<HoldingsFormat, HoldingsContentUpdate> resolveUpdateStrategy(HoldingsContentUpdate update) {
    switch (update.getOption()) {
      case PERMANENT_LOCATION:
      case TEMPORARY_LOCATION:
        return locationUpdateStrategy;
      default:
        throw new BulkEditException(String.format("Content updates for %s not implemented", update.getOption()));
    }
  }
}
