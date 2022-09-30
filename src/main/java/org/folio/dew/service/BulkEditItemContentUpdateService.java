package org.folio.dew.service;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.ItemContentUpdate.ActionEnum.CLEAR_FIELD;
import static org.folio.dew.domain.dto.ItemContentUpdate.ActionEnum.REPLACE_WITH;
import static org.folio.dew.domain.dto.ItemContentUpdate.OptionEnum.PERMANENT_LOAN_TYPE;
import static org.folio.dew.domain.dto.ItemContentUpdate.OptionEnum.PERMANENT_LOCATION;
import static org.folio.dew.domain.dto.ItemContentUpdate.OptionEnum.STATUS;
import static org.folio.dew.domain.dto.ItemContentUpdate.OptionEnum.TEMPORARY_LOCATION;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.PREVIEW_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.BULKEDIT_DIR_NAME;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.IDENTIFIER_TYPE;
import static org.folio.dew.utils.Constants.NO_CHANGE_MESSAGE;
import static org.folio.dew.utils.Constants.PREVIEW_PREFIX;
import static org.folio.dew.utils.Constants.STATUS_FIELD_CAN_NOT_CLEARED;
import static org.folio.dew.utils.Constants.STATUS_VALUE_NOT_ALLOWED;
import static org.folio.dew.utils.Constants.UPDATED_PREFIX;
import static org.folio.dew.utils.Constants.getWorkingDirectory;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.domain.dto.ItemContentUpdate;
import org.folio.dew.domain.dto.ItemContentUpdateCollection;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.FileOperationException;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.dew.utils.CsvHelper;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemContentUpdateService {
  private String workdir;

  @Value("${spring.application.name}")
  private String springApplicationName;

  private final ItemReferenceService itemReferenceService;
  private final RemoteFilesStorage remoteFilesStorage;
  private final LocalFilesStorage localFilesStorage;
  private final BulkEditProcessingErrorsService errorsService;

  @PostConstruct
  public void postConstruct() {
    workdir = getWorkingDirectory(springApplicationName, BULKEDIT_DIR_NAME);
  }

  public UpdatesResult<ItemFormat> processContentUpdates(JobCommand jobCommand, ItemContentUpdateCollection contentUpdates) {
    var outputFileName = workdir + UPDATED_PREFIX + FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION;
    try {
      log.info("Processing content updates for job id {}", jobCommand.getId());
      localFilesStorage.delete(outputFileName);
      remoteFilesStorage.downloadObject(FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION, outputFileName);
      var updateResult = new UpdatesResult<ItemFormat>();
      var records = CsvHelper.readRecordsFromStorage(localFilesStorage, outputFileName, ItemFormat.class, true);
      log.info("Reading of file {} complete, number of itemFormats: {}", outputFileName, records.size());
      updateResult.setTotal(records.size());
      var contentUpdated = applyContentUpdates(records, contentUpdates, jobCommand);
      updateResult.setEntitiesForPreview(contentUpdated.getPreview());
      var previewOutputFileName = workdir + PREVIEW_PREFIX + FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION;
      saveResultToFile(contentUpdated.getPreview(), jobCommand, previewOutputFileName, PREVIEW_FILE_NAME);
      saveResultToFile(contentUpdated.getUpdated(), jobCommand, outputFileName, UPDATED_FILE_NAME);
      jobCommand.setExportType(BULK_EDIT_UPDATE);
      return updateResult;
    } catch (Exception e) {
      var msg = String.format("Failed to read %s item records file for job id %s, reason: %s", outputFileName, jobCommand.getId(), e.getMessage());
      log.error(msg);
      throw new FileOperationException(msg);
    }
  }

  private void saveResultToFile(List<ItemFormat> itemFormats, JobCommand jobCommand, String outputFileName, String propertyValue) {
    try {
      CsvHelper.saveRecordsToStorage(localFilesStorage, itemFormats, ItemFormat.class, outputFileName);
      log.info("Saved {}", outputFileName);
      jobCommand.setJobParameters(new JobParametersBuilder(jobCommand.getJobParameters())
        .addString(propertyValue, outputFileName)
        .toJobParameters());
    } catch (Exception e) {
      var msg = String.format("Failed to write %s item records file for job id %s, reason: %s", outputFileName, jobCommand.getId(), e.getMessage());
      log.error(msg);
      throw new FileOperationException(msg);
    }
  }

  private ContentUpdateRecords<ItemFormat> applyContentUpdates(List<ItemFormat> itemFormats, ItemContentUpdateCollection contentUpdates, JobCommand jobCommand) {
    var result = new ContentUpdateRecords<ItemFormat>();
    for (ItemFormat itemFormat: itemFormats) {
      log.info("Applying updates to item id={}", itemFormat.getId());
      var updatedItemFormat = itemFormat;
      var errorMessage = new ErrorMessage();
      for (ItemContentUpdate contentUpdate: contentUpdates.getItemContentUpdates()) {
        updatedItemFormat = applyContentUpdate(updatedItemFormat, contentUpdate, errorMessage);
      }
      if (!Objects.equals(itemFormat, updatedItemFormat)) {
        if (isLocationChange(contentUpdates)) {
          updateEffectiveLocation(updatedItemFormat);
        }
        result.addToUpdated(updatedItemFormat);
        result.addToPreview(applyUpdatesForPreview(contentUpdates, updatedItemFormat));
      } else {
        var previewItemFormat = applyUpdatesForPreview(contentUpdates, itemFormat);
        result.addToPreview(previewItemFormat);
        if (Objects.equals(itemFormat, previewItemFormat)) {
          errorMessage.setValue(NO_CHANGE_MESSAGE);
        }
      }
      if (errorMessage.getValue() != null) {
        log.error(errorMessage.getValue());
        errorsService.saveErrorInCSV(jobCommand.getId().toString(), itemFormat.getIdentifier(jobCommand.getJobParameters().getString(IDENTIFIER_TYPE)), new BulkEditException(errorMessage.getValue()), FilenameUtils.getName(jobCommand.getJobParameters().getString(FILE_NAME)));
      }
    }
    return result;
  }


  private ItemFormat applyContentUpdate(ItemFormat itemFormat, ItemContentUpdate contentUpdate, ErrorMessage errorMessage) {
    if (REPLACE_WITH == contentUpdate.getAction()) {
      return applyReplaceWith(itemFormat, contentUpdate, errorMessage);
    } else if (CLEAR_FIELD == contentUpdate.getAction()) {
      if (STATUS == contentUpdate.getOption()) {
        errorMessage.setValue(STATUS_FIELD_CAN_NOT_CLEARED);
      } else if (PERMANENT_LOAN_TYPE == contentUpdate.getOption()) {
        errorMessage.setValue("Permanent loan type cannot be cleared");
      } else {
        return applyClearField(itemFormat, contentUpdate);
      }
    }
    return itemFormat;
  }

  private ItemFormat applyReplaceWith(ItemFormat itemFormat, ItemContentUpdate contentUpdate, ErrorMessage errorMessage) {
    var newValue = isEmpty(contentUpdate.getValue()) ? EMPTY : contentUpdate.getValue().toString();
    switch (contentUpdate.getOption()) {
    case PERMANENT_LOAN_TYPE:
      return replacePermanentLoanTypeIfAllowed(itemFormat, newValue, errorMessage);
    case TEMPORARY_LOAN_TYPE:
      return itemFormat.withTemporaryLoanType(newValue);
    case TEMPORARY_LOCATION:
      return itemFormat.withTemporaryLocation(newValue);
    case PERMANENT_LOCATION:
      return itemFormat.withPermanentLocation(newValue);
    case STATUS:
      return replaceStatusIfAllowed(itemFormat, newValue, errorMessage);
    default:
      return itemFormat;
    }
  }

  private ItemFormat replacePermanentLoanTypeIfAllowed(ItemFormat itemFormat, String newValue, ErrorMessage errorMessage) {
    if (newValue.isEmpty()) {
      errorMessage.setValue("Permanent loan type value cannot be empty");
      return itemFormat;
    } else {
      return itemFormat.withPermanentLoanType(newValue);
    }
  }

  private ItemFormat applyClearField(ItemFormat itemFormat, ItemContentUpdate contentUpdate) {
    switch (contentUpdate.getOption()) {
    case PERMANENT_LOCATION:
      return itemFormat.withPermanentLocation(EMPTY);
    case TEMPORARY_LOCATION:
      return itemFormat.withTemporaryLocation(EMPTY);
    case TEMPORARY_LOAN_TYPE:
      return itemFormat.withTemporaryLoanType(EMPTY);
    default:
      return itemFormat;
    }
  }

  private void updateEffectiveLocation(ItemFormat itemFormat) {
    if (isEmpty(itemFormat.getTemporaryLocation())) {
      itemFormat.setEffectiveLocation(isEmpty(itemFormat.getPermanentLocation()) ?
        itemReferenceService.getHoldingEffectiveLocationCodeById(itemFormat.getHoldingsRecordId()) :
        itemFormat.getPermanentLocation());
    } else {
      itemFormat.setEffectiveLocation(itemFormat.getTemporaryLocation());
    }
  }

  private boolean isLocationChange(ItemContentUpdateCollection contentUpdates) {
    return contentUpdates.getItemContentUpdates().stream()
      .anyMatch(update -> TEMPORARY_LOCATION == update.getOption() || PERMANENT_LOCATION == update.getOption());
  }

  private ItemFormat replaceStatusIfAllowed(ItemFormat itemFormat, String newStatus, ErrorMessage errorMessage) {
    var currentStatus = extractStatusName(itemFormat.getStatus());
    if (!currentStatus.equals(newStatus)) {
      if (itemReferenceService.getAllowedStatuses(currentStatus).contains(newStatus)) {
        return itemFormat.withStatus(String.join(ARRAY_DELIMITER, newStatus, dateToString(Date.from(LocalDateTime.now().atZone(UTC).toInstant()))));
      } else {
        var msg = String.format(STATUS_VALUE_NOT_ALLOWED, newStatus);
        errorMessage.setValue(msg);
      }
    }
    return itemFormat;
  }

  private String extractStatusName(String s) {
    var tokens = s.split(ARRAY_DELIMITER, -1);
    return tokens.length > 0 ? tokens[0] : EMPTY;
  }

  private ItemFormat applyUpdatesForPreview(ItemContentUpdateCollection contentUpdates, ItemFormat itemFormat) {
    var updatedItemFormat = applyStatusUpdateForPreview(contentUpdates, itemFormat);
    return applyLoanTypeUpdateForPreview(contentUpdates, updatedItemFormat);
  }

  private ItemFormat applyStatusUpdateForPreview(ItemContentUpdateCollection contentUpdates, ItemFormat itemFormat) {
    var statusUpdate = contentUpdates.getItemContentUpdates().stream()
      .filter(contentUpdate -> contentUpdate.getOption() == STATUS)
      .findFirst();
    if (statusUpdate.isPresent() && nonNull(statusUpdate.get().getValue()) && !extractStatusName(itemFormat.getStatus()).equals(statusUpdate.get().getValue())) {
      return itemFormat.withStatus(String.join(ARRAY_DELIMITER, statusUpdate.get().getValue().toString(), dateToString(Date.from(LocalDateTime.now().atZone(UTC).toInstant()))));
    }
    return itemFormat;
  }

  private ItemFormat applyLoanTypeUpdateForPreview(ItemContentUpdateCollection contentUpdates, ItemFormat itemFormat) {
    var update = contentUpdates.getItemContentUpdates().stream()
      .filter(contentUpdate -> contentUpdate.getOption() == PERMANENT_LOAN_TYPE)
      .findFirst();
    if (update.isPresent() && !Objects.equals(update.get().getValue(), itemFormat.getPermanentLoanType())) {
      return itemFormat.withPermanentLoanType(isNull(update.get().getValue()) ? EMPTY : update.get().getValue().toString());
    }
    return itemFormat;
  }
}
