package org.folio.dew.batch;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.utils.JobParameterNames;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public abstract class CsvPartitioner implements Partitioner {

  private static final int QUANTITY_PER_PARTITION = 250000;

  private final String outputFilePathTemplate;
  private long offset;
  private Long limit;

  protected CsvPartitioner(Long offset, Long limit, String tempOutputFilePath) {
    this.offset = offset == null ? 0 : offset;
    this.limit = limit;
    outputFilePathTemplate = createOutputFilePathTemplate(tempOutputFilePath);
  }

  @Override
  public Map<String, ExecutionContext> partition(int gridSize) {
    limit = limit == null ? getLimit() : limit;

    Map<String, ExecutionContext> partitionKeyContext = new HashMap<>();

    long numberOfPartitions = limit / QUANTITY_PER_PARTITION;
    if (numberOfPartitions == 0) {
      numberOfPartitions = 1;
    }

    long currentLimit;
    for (int i = 0; i < numberOfPartitions; i++) {
      String tempOutputFilePath = getPartitionOutputFilePath(i);
      currentLimit = limit - QUANTITY_PER_PARTITION >= QUANTITY_PER_PARTITION ? QUANTITY_PER_PARTITION : limit;

      ExecutionContext executionContext = new ExecutionContext();
      executionContext.putLong("offset", offset);
      executionContext.putLong("limit", currentLimit);
      executionContext.putString(JobParameterNames.TEMP_OUTPUT_FILE_PATH, tempOutputFilePath);

      offset += currentLimit;
      limit -= QUANTITY_PER_PARTITION;

      partitionKeyContext.put("Partition_" + i, executionContext);
    }

    return partitionKeyContext;
  }

  protected abstract long getLimit();

  private String getPartitionOutputFilePath(int partitionNumber) {
    return String.format(outputFilePathTemplate, partitionNumber);
  }

  private String createOutputFilePathTemplate(String tempOutputFilePath) {
    return tempOutputFilePath + "_%d.tmp";
  }

}
