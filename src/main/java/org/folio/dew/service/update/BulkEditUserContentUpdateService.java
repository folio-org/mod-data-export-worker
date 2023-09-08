package org.folio.dew.service.update;

import static java.lang.String.format;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.PREVIEW_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
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
import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateCollection;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.FileOperationException;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.ContentUpdateRecords;
import org.folio.dew.service.UpdatesResult;
import org.folio.dew.service.validation.UserContentUpdateValidatorService;
import org.folio.dew.utils.CsvHelper;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Log4j2
public class BulkEditUserContentUpdateService {
  private final RemoteFilesStorage remoteFilesStorage;
  private final BulkEditProcessingErrorsService errorsService;
  private final EmailUpdateStrategy emailUpdateStrategy;
  private final ExpirationDateUpdateStrategy expirationDateUpdateStrategy;
  private final PatronGroupUpdateStrategy patronGroupUpdateStrategy;
  private final UserContentUpdateValidatorService validatorService;

  public UpdatesResult<UserFormat> process(JobCommand jobCommand, UserContentUpdateCollection contentUpdates) {
    validatorService.validateContentUpdateCollection(contentUpdates);
    try {
      log.info("process:: Processing content updates for job id {}", jobCommand.getId());
      var fileName = FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION;
      var updatedFileName = jobCommand.getId() + PATH_SEPARATOR + UPDATED_PREFIX + fileName;
      var previewFileName = jobCommand.getId() + PATH_SEPARATOR + PREVIEW_PREFIX + fileName;
      var userFormats = CsvHelper.readRecordsFromStorage(remoteFilesStorage, jobCommand.getId() + PATH_SEPARATOR + fileName, UserFormat.class, true);
      log.info("process:: Reading of file {} complete, number of userFormats: {}", fileName, userFormats.size());
      var contentUpdatedUsers = applyContentUpdates(userFormats, contentUpdates, jobCommand);
      log.info("process:: Finished processing content updates: {} records, {} preview", contentUpdatedUsers.getUpdated().size(), contentUpdatedUsers.getPreview().size());
      CsvHelper.saveRecordsToStorage(remoteFilesStorage, contentUpdatedUsers.getUpdated(), UserFormat.class, updatedFileName);
      CsvHelper.saveRecordsToStorage(remoteFilesStorage, contentUpdatedUsers.getPreview(), UserFormat.class, previewFileName);
      jobCommand.setJobParameters(new JobParametersBuilder(jobCommand.getJobParameters())
        .addString(UPDATED_FILE_NAME, updatedFileName)
        .addString(PREVIEW_FILE_NAME, previewFileName)
        .toJobParameters());
      jobCommand.setExportType(BULK_EDIT_UPDATE);
      return new UpdatesResult<UserFormat>().withTotal(userFormats.size()).withEntitiesForPreview(contentUpdatedUsers.getPreview());
    } catch (Exception e) {
      var msg = String.format("I/O exception for job id %s, reason: %s", jobCommand.getId(), e.getMessage());
      log.error(msg);
      throw new FileOperationException(msg);
    }
  }

  private ContentUpdateRecords<UserFormat> applyContentUpdates(List<UserFormat> userFormats, UserContentUpdateCollection contentUpdateCollection, JobCommand jobCommand) {
    var updateResult = new ContentUpdateRecords<UserFormat>();
    userFormats.forEach(userFormat -> {
      var updatedUser = userFormat;
      for (UserContentUpdate contentUpdate: contentUpdateCollection.getUserContentUpdates()) {
        try {
          updatedUser = resolveUpdateStrategy(contentUpdate).applyUpdate(updatedUser, contentUpdate);
        } catch (BulkEditException e) {
          log.error("User content update {} was not applied for user {}, reason: {}", contentUpdate.getOption(), userFormat.getIdentifier(jobCommand.getJobParameters().getString(IDENTIFIER_TYPE)), e.getMessage());
          errorsService.saveErrorInCSV(jobCommand.getId().toString(), userFormat.getIdentifier(jobCommand.getJobParameters().getString(IDENTIFIER_TYPE)), e, FilenameUtils.getName(jobCommand.getJobParameters().getString(FILE_NAME)));
        }
      }
      if (!Objects.equals(updatedUser, userFormat)) {
        updateResult.addToUpdated(updatedUser);
      } else {
        errorsService.saveErrorInCSV(jobCommand.getId().toString(), userFormat.getIdentifier(jobCommand.getJobParameters().getString(IDENTIFIER_TYPE)), new BulkEditException(NO_CHANGE_MESSAGE), FilenameUtils.getName(jobCommand.getJobParameters().getString(FILE_NAME)));
      }
      updateResult.addToPreview(updatedUser);
    });
    return updateResult;
  }

  private UpdateStrategy<UserFormat, UserContentUpdate> resolveUpdateStrategy(UserContentUpdate update) {
    switch (update.getOption()) {
    case PATRON_GROUP:
      return patronGroupUpdateStrategy;
    case EXPIRATION_DATE:
      return expirationDateUpdateStrategy;
    case EMAIL_ADDRESS:
      return emailUpdateStrategy;
    default:
      throw new BulkEditException(format("Content updates for %s not implemented", update.getOption()));
    }
  }
}
