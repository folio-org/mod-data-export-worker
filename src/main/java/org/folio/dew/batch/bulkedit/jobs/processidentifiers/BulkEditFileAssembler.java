package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.FileOperationException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.folio.dew.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.dew.domain.dto.EntityType.INSTANCE;
import static org.folio.dew.domain.dto.EntityType.ITEM;
import static org.folio.dew.domain.dto.EntityType.USER;
import static org.folio.dew.domain.dto.JobParameterNames.AT_LEAST_ONE_MARC_EXISTS;
import static org.folio.dew.utils.Constants.NEW_LINE;
import static org.folio.dew.utils.Constants.UTF8_BOM;

@Component
@Log4j2
@RequiredArgsConstructor
public class BulkEditFileAssembler implements StepExecutionAggregator {

  @Value("${application.merge-csv-json-mrc-pool-size}")
  private int mergeCsvJsonMrcPoolSize;

  @Value("${application.minutes-for-merge}")
  private int numMinutesForMerge;

  @Override
  public void aggregate(StepExecution stepExecution, Collection<StepExecution> executions) {
    mergeCsvJsonMarcInParallel(stepExecution, executions);
  }

  private void mergeCsvJsonMarcInParallel(StepExecution stepExecution, Collection<StepExecution> executions) {
    ExecutorService exec = Executors.newFixedThreadPool(mergeCsvJsonMrcPoolSize);
    List<String> filesToDelete = new ArrayList<>();
    if (atLeastOneMarcExists(stepExecution)) {
      mergeMarc(stepExecution, executions, exec, filesToDelete);
    }
    mergeCsv(stepExecution, executions, exec, filesToDelete);
    mergeJson(stepExecution, executions, exec, filesToDelete);
    exec.shutdown();
    try {
      if (exec.awaitTermination(numMinutesForMerge, TimeUnit.MINUTES)) {
        log.info("Merge csv, json and mrc files has been completed successfully.");
        removePartFiles(filesToDelete);
      } else {
        var errorMsg = "Merge csv, json and mrc files exceeded allowed %d minutes".formatted(numMinutesForMerge);
        log.error(errorMsg);
        throw new BulkEditException(errorMsg);
      }
    } catch (InterruptedException e) {
      log.error(e);
      Thread.currentThread().interrupt();
      throw new FileOperationException(e);
    }
  }

  private void mergeMarc(StepExecution stepExecution, Collection<StepExecution> executions, ExecutorService exec, List<String> filesToDelete) {
    if (isInstanceJob(stepExecution)) {
      exec.execute(() -> {
        var marcFileParts = executions.stream()
          .map(e -> e.getExecutionContext().getString(JobParameterNames.TEMP_OUTPUT_MARC_PATH)).toList();
        mergePartFiles(stepExecution, JobParameterNames.TEMP_LOCAL_MARC_PATH, ".mrc", marcFileParts, false);
        filesToDelete.addAll(marcFileParts);
      });
    }
  }

  private void mergeCsv(StepExecution stepExecution, Collection<StepExecution> executions, ExecutorService exec, List<String> filesToDelete) {
    exec.execute(() -> {
      var csvFileParts = executions.stream()
        .map(e -> e.getExecutionContext().getString(JobParameterNames.TEMP_OUTPUT_CSV_PATH)).toList();
      mergePartFiles(stepExecution, JobParameterNames.TEMP_LOCAL_FILE_PATH, "", csvFileParts, true);
      filesToDelete.addAll(csvFileParts);
    });
  }

  private void mergeJson(StepExecution stepExecution, Collection<StepExecution> executions, ExecutorService exec, List<String> filesToDelete) {
    exec.execute(() -> {
      var jsonFileParts = executions.stream()
        .map(e -> e.getExecutionContext().getString(JobParameterNames.TEMP_OUTPUT_JSON_PATH)).toList();
      mergePartFiles(stepExecution, JobParameterNames.TEMP_LOCAL_FILE_PATH, ".json", jsonFileParts, false);
      filesToDelete.addAll(jsonFileParts);
    });
  }

  private void mergePartFiles(StepExecution stepExecution, String jobParamOfOutputFileName, String outputFileExtension, List<String> fileParts, boolean csv) {
    try(OutputStream out = Files.newOutputStream(Paths.get(stepExecution.getJobExecution().getJobParameters().getString(jobParamOfOutputFileName) + outputFileExtension),
      StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      if (csv) {
        writeColumnHeaders(stepExecution, out);
      }
      for (String part: fileParts) {
        Files.copy(Path.of(part), out);
      }
    } catch (Exception e) {
      log.error("Problem occurred while merging part files", e);
      throw new FileOperationException(e);
    }
  }

  private void writeColumnHeaders(StepExecution stepExecution, OutputStream out) throws IOException {
    var columnHeaders = getColumnHeaders(stepExecution);
    out.write(columnHeaders.getBytes(StandardCharsets.UTF_8));
  }

  private String getColumnHeaders(StepExecution stepExecution) {
    var jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
    if (jobName.endsWith(INSTANCE.getValue())) {
      return UTF8_BOM + InstanceFormat.getInstanceColumnHeaders() + NEW_LINE;
    } else if (jobName.endsWith(HOLDINGS_RECORD.getValue())) {
      return UTF8_BOM + HoldingsFormat.getHoldingsColumnHeaders() + NEW_LINE;
    } else if (jobName.endsWith(ITEM.getValue())) {
      return UTF8_BOM + ItemFormat.getItemColumnHeaders() + NEW_LINE;
    } else if (jobName.endsWith(USER.getValue())) {
      return UTF8_BOM + UserFormat.getUserColumnHeaders() + NEW_LINE;
    }
    throw new UnsupportedOperationException(jobName + " is not supported.");
  }

  private boolean isInstanceJob(StepExecution stepExecution) {
    var jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
    return jobName.endsWith(INSTANCE.getValue());
  }

  private void removePartFiles(List<String> partFiles) {
    ExecutorService exec = Executors.newCachedThreadPool();
    exec.execute(() -> {
      partFiles.forEach(file -> {
        try {
          Files.delete(Path.of(file));
        } catch (IOException e) {
          log.error("Error occurred while deleting the part files", e);
          throw new FileOperationException(e);
        }
      });
      log.info("All {} part files have been deleted successfully.", partFiles.size());
    });
  }

  private boolean atLeastOneMarcExists(StepExecution stepExecution) {
    return stepExecution.getJobExecution().getExecutionContext().containsKey(AT_LEAST_ONE_MARC_EXISTS);
  }
}
