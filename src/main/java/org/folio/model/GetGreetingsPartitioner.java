package org.folio.model;

import org.apache.commons.io.FilenameUtils;
import org.folio.model.entities.constants.JobParameterNames;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

public class GetGreetingsPartitioner implements Partitioner {

    private int NUMBER_OF_GREETINGS_PER_PARTITION = 250000;

    private int greetingsOffset;

    private int greetingsLimit;

    private String outputFilePathTemplate;

    public GetGreetingsPartitioner(int greetingsOffset, int greetingsLimit, String outputFilePath) {
        this.greetingsOffset = greetingsOffset;
        this.greetingsLimit = greetingsLimit;
        this.outputFilePathTemplate = createOutputFilePathTemplate(outputFilePath);
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitionKey_Context = new HashMap<>();

        int numberOfPartitions = this.greetingsLimit / NUMBER_OF_GREETINGS_PER_PARTITION;
        if (numberOfPartitions == 0) {
            numberOfPartitions = 1;
        }

        int currentGreetingsLimit;
        for (int i = 0; i < numberOfPartitions; i++) {
            String outputFilePath = getPartitionOutputFilePath(i);
            currentGreetingsLimit =
                    this.greetingsLimit - NUMBER_OF_GREETINGS_PER_PARTITION >= NUMBER_OF_GREETINGS_PER_PARTITION ? NUMBER_OF_GREETINGS_PER_PARTITION : this.greetingsLimit;

            ExecutionContext executionContext = new ExecutionContext();
            executionContext.putLong("greetingsOffset", this.greetingsOffset);
            executionContext.putLong("greetingsLimit", currentGreetingsLimit);
            executionContext.putString(JobParameterNames.OUTPUT_FILE_PATH, outputFilePath);

            // TODO remove this line
            System.out.println("Partition created: " + i + " Offset: " + this.greetingsOffset + " Limit: " + currentGreetingsLimit + " Output file path: " + outputFilePath);

            this.greetingsOffset += currentGreetingsLimit;
            this.greetingsLimit -= NUMBER_OF_GREETINGS_PER_PARTITION;

            partitionKey_Context.put("Partition_" + i, executionContext);
        }

        return partitionKey_Context;
    }

    private String getPartitionOutputFilePath(int partitionNumber) {
        String outputFileName = String.format(this.outputFilePathTemplate, partitionNumber);
        return outputFileName;
    }

    private String createOutputFilePathTemplate(String outputFilePath) {
        String outputFilePathTemplate =
                FilenameUtils.getFullPath(outputFilePath) + FilenameUtils.getBaseName(outputFilePath) + "_%d." + FilenameUtils.getExtension(outputFilePath) + ".tmp";
        return outputFilePathTemplate;
    }
}
