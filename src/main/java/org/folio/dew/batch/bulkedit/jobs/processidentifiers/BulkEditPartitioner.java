package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static java.util.Objects.nonNull;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.error.FileOperationException;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class BulkEditPartitioner implements Partitioner {

  private final String outputCsvPathTemplate;
  private final String outputJsonPathTemplate;
  private final String outputMarcPathTemplate;
  private final String uploadedFileName;
  private Long offset;
  private Long limit;

  @Value("${application.quantity-per-partition}")
  private int quantityPerPartition;

  protected BulkEditPartitioner(String tempOutputCsvPath, String tempOutputJsonPath, String tempOutputMarcPath, String uploadedFileName) {
    this.uploadedFileName = uploadedFileName;
    offset = 0l;
    limit = getLimit();
    outputCsvPathTemplate = tempOutputCsvPath + "_%d.csv";
    outputJsonPathTemplate = tempOutputJsonPath + "_%d.json";
    outputMarcPathTemplate = tempOutputMarcPath == null ? null : tempOutputMarcPath + "_%d.mrc";
  }

  @Override
  public Map<String, ExecutionContext> partition(int gridSize) {

    Map<String, ExecutionContext> result = new HashMap<>();

    long numberOfPartitions = limit / quantityPerPartition;
    if (numberOfPartitions == 0) {
      numberOfPartitions = 1;
    }

    long currentLimit;
    for (var i = 0; i < numberOfPartitions; i++) {
      String tempOutputCsvPath = outputCsvPathTemplate.formatted(i);
      String tempOutputJsonPath = outputJsonPathTemplate.formatted(i);
      String tempOutputMarcPath = outputMarcPathTemplate == null ? null : outputMarcPathTemplate.formatted(i);
      currentLimit = limit - quantityPerPartition >= quantityPerPartition ? quantityPerPartition : limit;

      var executionContext = new ExecutionContext();
      executionContext.putLong("offset", offset);
      executionContext.putLong("limit", currentLimit);
      executionContext.putLong("partition", i);
      executionContext.putString(JobParameterNames.TEMP_OUTPUT_CSV_PATH, tempOutputCsvPath);
      if (nonNull(tempOutputMarcPath)) {
        executionContext.putString(JobParameterNames.TEMP_OUTPUT_MARC_PATH, tempOutputMarcPath);
      }
      executionContext.putString(JobParameterNames.TEMP_OUTPUT_JSON_PATH, tempOutputJsonPath);
      result.put("Partition_" + i, executionContext);

      log.info("Partition {}: offset {}, limit {}, tempOutputPath {}, {}, {}.",
        i, offset, currentLimit, tempOutputCsvPath, tempOutputJsonPath, tempOutputMarcPath);

      offset += currentLimit;
      limit -= quantityPerPartition;
    }
    return result;
  }

  private Long getLimit() {
    try {
      return (long) Files.readAllLines(Path.of(uploadedFileName)).size();
    } catch (IOException e) {
      log.error("Problem occurred in partitioner while reading input file to get limit", e);
      throw new FileOperationException(e);
    }
  }
}
