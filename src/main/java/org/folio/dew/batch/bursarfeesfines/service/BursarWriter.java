package org.folio.dew.batch.bursarfeesfines.service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.repository.LocalFilesStorage;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.WritableResource;

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

  private int aggregateTotalAmount;

  @Override
  public void write(Chunk<? extends String> items) throws Exception {
    // Get header and footer and convert to string
    String header = jobConfig
      .getHeader()
      .stream()
      .map(token -> BursarTokenFormatter.formatHeaderFooterToken(token))
      .collect(Collectors.joining());

    String footer = jobConfig
      .getFooter()
      .stream()
      .map(token -> BursarTokenFormatter.formatHeaderFooterToken(token))
      .collect(Collectors.joining());

    localFilesStorage.write(
      resource.getFilename(),
      header.getBytes(StandardCharsets.UTF_8)
    );

    StringBuilder lines = new StringBuilder();
    for (String item : items) {
      lines.append(item);
    }
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
