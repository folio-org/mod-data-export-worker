package org.folio.dew.batch.circulationlog;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.des.domain.dto.JobParameterNames;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CirculationLogPartitioner implements Partitioner {

  private static final int QUANTITY_PER_PARTITION = 250000;

  private int offset;
  private int limit;
  private final String outputFilePathTemplate;

  public CirculationLogPartitioner(int offset, int limit, String outputFilePath) {
    this.offset = offset;
    this.limit = limit;
    this.outputFilePathTemplate = createOutputFilePathTemplate(outputFilePath);
  }

  @Override
  public Map<String, ExecutionContext> partition(int gridSize) {
    Map<String, ExecutionContext> partitionKeyContext = new HashMap<>();

    int numberOfPartitions = this.limit / QUANTITY_PER_PARTITION;
    if (numberOfPartitions == 0) {
      numberOfPartitions = 1;
    }

    int currentLimit;
    for (int i = 0; i < numberOfPartitions; i++) {
      String outputFilePath = getPartitionOutputFilePath(i);
      currentLimit = this.limit - QUANTITY_PER_PARTITION >= QUANTITY_PER_PARTITION ? QUANTITY_PER_PARTITION : this.limit;

      ExecutionContext executionContext = new ExecutionContext();
      executionContext.putLong("circulationLogOffset", this.offset);
      executionContext.putLong("circulationLogLimit", currentLimit);
      executionContext.putString(JobParameterNames.OUTPUT_FILE_PATH, outputFilePath);

      log.debug(
          "Partition created: " + i + " Offset: " + this.offset + " Limit: " + currentLimit + " Output file path: " + outputFilePath);

      this.offset += currentLimit;
      this.limit -= QUANTITY_PER_PARTITION;

      partitionKeyContext.put("Partition_" + i, executionContext);
    }

    return partitionKeyContext;
  }

  private String getPartitionOutputFilePath(int partitionNumber) {
    return String.format(this.outputFilePathTemplate, partitionNumber);
  }

  private String createOutputFilePathTemplate(String outputFilePath) {
    return FilenameUtils.getFullPath(outputFilePath) + FilenameUtils.getBaseName(
        outputFilePath) + "_%d." + FilenameUtils.getExtension(outputFilePath) + ".tmp";
  }

}
