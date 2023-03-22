package org.folio.dew.batch.bursarfeesfines.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.repository.LocalFilesStorage;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.WritableResource;

@Log4j2
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BursarWriter
  extends AbstractItemStreamItemWriter<String>
  implements ResourceAwareItemWriterItemStream<String> {

  protected LineAggregator<String> lineAggregator;
  private LocalFilesStorage localFilesStorage;
  private WritableResource resource;

  @Value("#{jobExecutionContext['jobConfig']}")
  private BursarExportJob jobConfig;

  @Value("#{jobExecutionContext['totalAmount']}")
  private BigDecimal aggregateTotalAmount;

  private int aggregateNumRows;

  @Override
  public void write(Chunk<? extends String> items) throws Exception {
    // Build the items into lines to write to file
    // Also aggregate the number of rows
    // StepExecution stepExecution = StepSynchronizationManager
    //   .getContext()
    //   .getStepExecution();

    log.info("Total amount fee is {}", aggregateTotalAmount.toString());
    aggregateNumRows = 0;
    StringBuilder lines = new StringBuilder();
    for (String item : items) {
      ++aggregateNumRows;
      lines.append(item);
    }

    // Get header and footer and convert to string
    String header = jobConfig
      .getHeader()
      .stream()
      .map(token ->
        BursarTokenFormatter.formatHeaderFooterToken(
          token,
          aggregateNumRows,
          aggregateTotalAmount
        )
      )
      .collect(Collectors.joining());

    String footer = jobConfig
      .getFooter()
      .stream()
      .map(token ->
        BursarTokenFormatter.formatHeaderFooterToken(
          token,
          aggregateNumRows,
          aggregateTotalAmount
        )
      )
      .collect(Collectors.joining());

    localFilesStorage.write(
      resource.getFilename(),
      header.getBytes(StandardCharsets.UTF_8)
    );

    localFilesStorage.append(
      resource.getFilename(),
      lines.toString().getBytes(StandardCharsets.UTF_8)
    );

    localFilesStorage.append(
      resource.getFilename(),
      footer.getBytes(StandardCharsets.UTF_8)
    );
  }
}
