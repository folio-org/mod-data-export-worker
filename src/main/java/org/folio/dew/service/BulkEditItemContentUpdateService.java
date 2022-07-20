package org.folio.dew.service;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.ContentUpdate.ActionEnum.CLEAR_FIELD;
import static org.folio.dew.domain.dto.ContentUpdate.ActionEnum.REPLACE_WITH;
import static org.folio.dew.domain.dto.ContentUpdate.OptionEnum.PERMANENT_LOCATION;
import static org.folio.dew.domain.dto.ContentUpdate.OptionEnum.STATUS;
import static org.folio.dew.domain.dto.ContentUpdate.OptionEnum.TEMPORARY_LOCATION;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.PREVIEW_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.IDENTIFIER_TYPE;
import static org.folio.dew.utils.Constants.NO_CHANGE_MESSAGE;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.PREVIEW_PREFIX;
import static org.folio.dew.utils.Constants.STATUS_FIELD_CAN_NOT_CLEARED;
import static org.folio.dew.utils.Constants.STATUS_VALUE_NOT_ALLOWED;
import static org.folio.dew.utils.Constants.TMP_DIR_PROPERTY;
import static org.folio.dew.utils.Constants.UPDATED_PREFIX;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.domain.dto.ContentUpdate;
import org.folio.dew.domain.dto.ContentUpdateCollection;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.FileOperationException;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.utils.CsvHelper;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
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
  private final MinIOObjectStorageRepository repository;
  private final BulkEditProcessingErrorsService errorsService;

  @PostConstruct
  public void postConstruct() {
    workdir = System.getProperty(TMP_DIR_PROPERTY) + PATH_SEPARATOR + springApplicationName + PATH_SEPARATOR;
  }

  public ItemUpdatesResult processContentUpdates(JobCommand jobCommand, ContentUpdateCollection contentUpdates) {
    var outputFileName = workdir + UPDATED_PREFIX + FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION;
    try {
      log.info("Processing content updates for job id {}", jobCommand.getId());
      Files.deleteIfExists(Path.of(outputFileName));
      repository.downloadObject(FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION, outputFileName);
      var updateResult = new ItemUpdatesResult();
      var records = CsvHelper.readRecordsFromFile(outputFileName, ItemFormat.class, true);
      log.info("Reading of file {} complete, number of itemFormats: {}", outputFileName, records.size());
      updateResult.setTotal(records.size());
      var contentUpdated = applyContentUpdates(records, contentUpdates, jobCommand);
      updateResult.setItemsForPreview(contentUpdated.getPreview());
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
      CsvHelper.saveRecordsToCsv(itemFormats, ItemFormat.class, outputFileName);
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

  private ContentUpdateRecords applyContentUpdates(List<ItemFormat> itemFormats, ContentUpdateCollection contentUpdates, JobCommand jobCommand) {
    var result = new ContentUpdateRecords();
    for (ItemFormat itemFormat: itemFormats) {
      log.info("Applying updates to item id={}", itemFormat.getId());
      var updatedItemFormat = itemFormat;
      var errorMessage = new ErrorMessage();
      for (ContentUpdate contentUpdate: contentUpdates.getContentUpdates()) {
        updatedItemFormat = applyContentUpdate(updatedItemFormat, contentUpdate, errorMessage);
      }
      if (!Objects.equals(itemFormat, updatedItemFormat)) {
        if (isLocationChange(contentUpdates)) {
          updateEffectiveLocation(updatedItemFormat);
        }
        result.addToUpdated(updatedItemFormat);
        result.addToPreview(updatedItemFormat);
      } else {
        var statusUpdate = contentUpdates.getContentUpdates().stream()
            .filter(contentUpdate -> contentUpdate.getOption() == STATUS)
            .findFirst();
        if (statusUpdate.isPresent() && nonNull(statusUpdate.get().getValue()) && !extractStatusName(itemFormat.getStatus()).equals(statusUpdate.get().getValue())) {
          var status = statusUpdate.get().getValue();
          result.addToPreview(itemFormat.withStatus(String.join(ARRAY_DELIMITER, status.toString(), dateToString(Date.from(LocalDateTime.now().atZone(UTC).toInstant())))));
        } else {
          result.addToPreview(itemFormat);
          errorMessage.setValue(NO_CHANGE_MESSAGE);
        }
      }
      if (errorMessage.getValue() != null) {
        log.error(errorMessage);
        errorsService.saveErrorInCSV(jobCommand.getId().toString(), itemFormat.getIdentifier(jobCommand.getJobParameters().getString(IDENTIFIER_TYPE)), new BulkEditException(errorMessage.getValue()), FilenameUtils.getName(jobCommand.getJobParameters().getString(FILE_NAME)));
      }
    }
    return result;
  }


  private ItemFormat applyContentUpdate(ItemFormat itemFormat, ContentUpdate contentUpdate, ErrorMessage errorMessage) {
    if (REPLACE_WITH == contentUpdate.getAction()) {
      return applyReplaceWith(itemFormat, contentUpdate, errorMessage);
    } else if (CLEAR_FIELD == contentUpdate.getAction()) {
      if (STATUS == contentUpdate.getOption()) {
        errorMessage.setValue(STATUS_FIELD_CAN_NOT_CLEARED);
      } else {
        return applyClearField(itemFormat, contentUpdate);
      }
    }
    return itemFormat;
  }

  private ItemFormat applyReplaceWith(ItemFormat itemFormat, ContentUpdate contentUpdate, ErrorMessage errorMessage) {
    if (TEMPORARY_LOCATION == contentUpdate.getOption()) {
      return itemFormat.withTemporaryLocation(isNull(contentUpdate.getValue()) ? EMPTY : contentUpdate.getValue().toString());
    } else if (PERMANENT_LOCATION == contentUpdate.getOption()) {
      return itemFormat.withPermanentLocation(isNull(contentUpdate.getValue()) ? EMPTY : contentUpdate.getValue().toString());
    } else if (STATUS == contentUpdate.getOption()) {
      return replaceStatusIfAllowed(itemFormat, contentUpdate.getValue(), errorMessage);
    }
    return itemFormat;
  }

  private ItemFormat applyClearField(ItemFormat itemFormat, ContentUpdate contentUpdate) {
    if (TEMPORARY_LOCATION == contentUpdate.getOption()) {
      return itemFormat.withTemporaryLocation(EMPTY);
    } else if (PERMANENT_LOCATION == contentUpdate.getOption()) {
      return itemFormat.withPermanentLocation(EMPTY);
    }
    return itemFormat;
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

  private boolean isLocationChange(ContentUpdateCollection contentUpdates) {
    return contentUpdates.getContentUpdates().stream()
      .anyMatch(update -> TEMPORARY_LOCATION == update.getOption() || PERMANENT_LOCATION == update.getOption());
  }

  private ItemFormat replaceStatusIfAllowed(ItemFormat itemFormat, Object value, ErrorMessage errorMessage) {
    var currentStatus = extractStatusName(itemFormat.getStatus());
    var newStatus = isNull(value) ? EMPTY : value.toString();
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
}
