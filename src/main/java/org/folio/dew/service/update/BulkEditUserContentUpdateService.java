package org.folio.dew.service.update;

import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.PREVIEW_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.IDENTIFIER_TYPE;
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
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.ContentUpdateRecords;
import org.folio.dew.service.UpdatesResult;
import org.folio.dew.utils.CsvHelper;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Log4j2
public class BulkEditUserContentUpdateService {
  private final MinIOObjectStorageRepository repository;
  private final BulkEditProcessingErrorsService errorsService;

  public UpdatesResult<UserFormat> process(JobCommand jobCommand, UserContentUpdateCollection contentUpdates) {
    try {
      var fileName = FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION;
      var updatedFileName = UPDATED_PREFIX + fileName;
      var previewFileName = PREVIEW_PREFIX + fileName;
      var userFormats = CsvHelper.readRecordsFromMinio(repository, fileName, UserFormat.class, true);
      var contentUpdatedUsers = applyContentUpdates(userFormats, contentUpdates, jobCommand);
      CsvHelper.saveRecordsToMinio(repository, contentUpdatedUsers.getUpdated(), UserFormat.class, updatedFileName);
      CsvHelper.saveRecordsToMinio(repository, contentUpdatedUsers.getPreview(), UserFormat.class, previewFileName);
      jobCommand.setJobParameters(new JobParametersBuilder(jobCommand.getJobParameters())
        .addString(UPDATED_FILE_NAME, updatedFileName)
        .addString(PREVIEW_FILE_NAME, previewFileName)
        .toJobParameters());
      jobCommand.setExportType(BULK_EDIT_UPDATE);
      return new UpdatesResult<UserFormat>().withTotal(userFormats.size()).withUsersForPreview(contentUpdatedUsers.getPreview());
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
      var updatedPreview = userFormat;
      for (UserContentUpdate contentUpdate: contentUpdateCollection.getUserContentUpdates()) {
        try {
          var updateStrategy = resolveUpdateStrategy(contentUpdate);
          updatedPreview = updateStrategy.applyUpdate(updatedPreview, contentUpdate, true);
          updatedUser = updateStrategy.applyUpdate(updatedUser, contentUpdate, false);
        } catch (BulkEditException e) {
          log.error("Failed to update {} for user id={}, reason: {}", contentUpdate.getOption(), userFormat.getId(), e.getMessage());
          errorsService.saveErrorInCSV(jobCommand.getId().toString(), userFormat.getIdentifier(jobCommand.getJobParameters().getString(IDENTIFIER_TYPE)), e, FilenameUtils.getName(jobCommand.getJobParameters().getString(FILE_NAME)));
        }
      }
      if (!Objects.equals(updatedUser, userFormat)) {
        updateResult.addToUpdated(updatedUser);
      }
      updateResult.addToPreview(updatedPreview);
    });
    return updateResult;
  }

  private UpdateStrategy<UserFormat, UserContentUpdate> resolveUpdateStrategy(UserContentUpdate update) {
    switch (update.getOption()) {
    case PATRON_GROUP:
      return new PatronGroupUpdateStrategy();
    case EXPIRATION_DATE:
      return new ExpirationDateUpdateStrategy();
    case EMAIL_ADDRESS:
      return new EmailUpdateStrategy();
    default:
      throw new BulkEditException(String.format("Content updates for %s not implemented", update.getOption()));
    }
  }
}
