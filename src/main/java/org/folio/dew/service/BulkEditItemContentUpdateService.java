package org.folio.dew.service;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.ContentUpdate.ActionEnum.CLEAR_FIELD;
import static org.folio.dew.domain.dto.ContentUpdate.ActionEnum.REPLACE_WITH;
import static org.folio.dew.domain.dto.ContentUpdate.OptionEnum.PERMANENT_LOCATION;
import static org.folio.dew.domain.dto.ContentUpdate.OptionEnum.TEMPORARY_LOCATION;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.TMP_DIR_PROPERTY;
import static org.folio.dew.utils.Constants.UPDATED_PREFIX;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.domain.dto.ContentUpdate;
import org.folio.dew.domain.dto.ContentUpdateCollection;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.error.FileOperationException;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.utils.CsvHelper;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemContentUpdateService {
  private String workdir;
  private String outputFileName;
  @Value("${spring.application.name}")
  private String springApplicationName;

  private final ItemReferenceService itemReferenceService;
  private final MinIOObjectStorageRepository repository;

  @PostConstruct
  public void postConstruct() {
    workdir = System.getProperty(TMP_DIR_PROPERTY) + PATH_SEPARATOR + springApplicationName + PATH_SEPARATOR;
  }

  public List<ItemFormat> processContentUpdates(JobCommand jobCommand, ContentUpdateCollection contentUpdates) {
    try {
      log.info("Processing content updates for job id {}", jobCommand.getId());
      outputFileName = workdir + UPDATED_PREFIX + FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION;
      Files.deleteIfExists(Path.of(outputFileName));
      repository.downloadObject(FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION, outputFileName);
      var updatedItems = applyContentUpdates(CsvHelper.readRecordsFromFile(outputFileName, ItemFormat.class, true), contentUpdates);
      saveResultToFile(updatedItems, jobCommand);
      return updatedItems;
    } catch (Exception e) {
      var msg = String.format("Failed to read %s item records file for job id %s, reason: %s", outputFileName, jobCommand.getId(), e.getMessage());
      log.error(msg);
      throw new FileOperationException(msg);
    }
  }

  private void saveResultToFile(List<ItemFormat> itemFormats, JobCommand jobCommand) {
    try {
      CsvHelper.saveRecordsToCsv(itemFormats, ItemFormat.class, outputFileName);
      jobCommand.setJobParameters(new JobParametersBuilder(jobCommand.getJobParameters())
        .addString(UPDATED_FILE_NAME, outputFileName)
        .toJobParameters());
    } catch (Exception e) {
      var msg = String.format("Failed to write %s item records file for job id %s, reason: %s", outputFileName, jobCommand.getId(), e.getMessage());
      log.error(msg);
      throw new FileOperationException(msg);
    }
  }

  private List<ItemFormat> applyContentUpdates(List<ItemFormat> itemFormats, ContentUpdateCollection contentUpdates) {
    List<ItemFormat> result = new ArrayList<>();
    itemFormats.forEach(itemFormat -> {
      contentUpdates.getContentUpdates().forEach(contentUpdate -> applyContentUpdate(itemFormat, contentUpdate));
      result.add(itemFormat);
    });
    return result;
  }

  private void applyContentUpdate(ItemFormat itemFormat, ContentUpdate contentUpdate) {
    if (REPLACE_WITH == contentUpdate.getAction()) {
      applyReplaceWith(itemFormat, contentUpdate);
    } else if (CLEAR_FIELD == contentUpdate.getAction()) {
      applyClearField(itemFormat, contentUpdate);
    }
  }

  private void applyReplaceWith(ItemFormat itemFormat, ContentUpdate contentUpdate) {
    if (TEMPORARY_LOCATION == contentUpdate.getOption()) {
      itemFormat.setTemporaryLocation(isNull(contentUpdate.getValue()) ? EMPTY : contentUpdate.getValue().toString());
    } else if (PERMANENT_LOCATION == contentUpdate.getOption()) {
      itemFormat.setPermanentLocation(isNull(contentUpdate.getValue()) ? EMPTY : contentUpdate.getValue().toString());
    }
    calculateEffectiveLocation(itemFormat);
  }

  private void applyClearField(ItemFormat itemFormat, ContentUpdate contentUpdate) {
    if (TEMPORARY_LOCATION == contentUpdate.getOption()) {
      itemFormat.setTemporaryLocation(EMPTY);
    } else if (PERMANENT_LOCATION == contentUpdate.getOption()) {
      itemFormat.setPermanentLocation(EMPTY);
    }
    calculateEffectiveLocation(itemFormat);
  }

  private void calculateEffectiveLocation(ItemFormat itemFormat) {
    if (isEmpty(itemFormat.getTemporaryLocation())) {
      itemFormat.setEffectiveLocation(isEmpty(itemFormat.getPermanentLocation()) ?
        itemReferenceService.getHoldingEffectiveLocationCodeById(itemFormat.getHoldingsRecordId()) :
        itemFormat.getPermanentLocation());
    } else {
      itemFormat.setEffectiveLocation(itemFormat.getTemporaryLocation());
    }
  }
}
