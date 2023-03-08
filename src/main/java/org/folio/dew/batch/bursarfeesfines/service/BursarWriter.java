package org.folio.dew.batch.bursarfeesfines.service;

import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.folio.dew.repository.LocalFilesStorage;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
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

  private String header;
  private String footer;

  private int aggregateTotalAmount;

  @Override
  public void write(Chunk<? extends String> items) throws Exception {
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
