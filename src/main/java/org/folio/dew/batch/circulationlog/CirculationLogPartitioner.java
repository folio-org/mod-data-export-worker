package org.folio.dew.batch.circulationlog;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.utils.JobParameterNames;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CirculationLogPartitioner implements Partitioner {

  private static final int QUANTITY_PER_PARTITION = 250000;

  private final String outputFilePathTemplate;
  private int offset;
  private int limit;

  public CirculationLogPartitioner(int offset, int limit, String tempOutputFilePath) {
    this.offset = offset;
    this.limit = limit;
    outputFilePathTemplate = createOutputFilePathTemplate(tempOutputFilePath);
  }

  @Override
  public Map<String, ExecutionContext> partition(int gridSize) {
    Map<String, ExecutionContext> partitionKeyContext = new HashMap<>();

    int numberOfPartitions = limit / QUANTITY_PER_PARTITION;
    if (numberOfPartitions == 0) {
      numberOfPartitions = 1;
    }

    int currentLimit;
    for (int i = 0; i < numberOfPartitions; i++) {
      String tempOutputFilePath = getPartitionOutputFilePath(i);
      currentLimit = limit - QUANTITY_PER_PARTITION >= QUANTITY_PER_PARTITION ? QUANTITY_PER_PARTITION : limit;

      ExecutionContext executionContext = new ExecutionContext();
      executionContext.putLong("circulationLogOffset", offset);
      executionContext.putLong("circulationLogLimit", currentLimit);
      executionContext.putString(JobParameterNames.TEMP_OUTPUT_FILE_PATH, tempOutputFilePath);

      log.debug(
          "Partition created: " + i + " Offset: " + offset + " Limit: " + currentLimit + " Output file path: " + tempOutputFilePath);

      offset += currentLimit;
      limit -= QUANTITY_PER_PARTITION;

      partitionKeyContext.put("Partition_" + i, executionContext);
    }

    return partitionKeyContext;
  }

  private String getPartitionOutputFilePath(int partitionNumber) {
    return String.format(outputFilePathTemplate, partitionNumber);
  }

  private String createOutputFilePathTemplate(String tempOutputFilePath) {
    return tempOutputFilePath + "_%d.tmp";
  }

}
