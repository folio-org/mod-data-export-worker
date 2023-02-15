package org.folio.dew.batch.bursarfeesfines.service;


import lombok.extern.log4j.Log4j2;
import org.folio.dew.error.FileOperationException;
import org.folio.dew.repository.LocalFilesStorage;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Log4j2
public class BursarWriter<T> extends AbstractItemStreamItemWriter<T>
  implements ResourceAwareItemWriterItemStream<T> {

  protected LineAggregator<T> lineAggregator;

  private LocalFilesStorage localFilesStorage;

  private WritableResource resource;

  private String header;

  private String lineSeparator;


  public void setLineAggregator(LineAggregator<T> lineAggregator) {
    this.lineAggregator = lineAggregator;
  }

  public void setLineSeparator(String lineSeparator) {
    this.lineSeparator = lineSeparator;
  }

  public void setLocalFilesStorage(LocalFilesStorage localFilesStorage) {
    this.localFilesStorage = localFilesStorage;
  }

  @Override
  public void setResource(WritableResource resource) {
    this.resource = resource;
  }

  public void setHeader(String header) {
    this.header = header;
  }


  @Override
  public void write(Chunk<? extends T> items) throws Exception {
    StringBuilder lines = new StringBuilder();
    for (T item : items) {
      lines.append(this.lineAggregator.aggregate(item)).append(this.lineSeparator);
    }
    localFilesStorage.append(resource.getFilename(), lines.toString().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    try {
      localFilesStorage.write(resource.getFilename(), (header + lineSeparator).getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new FileOperationException(e);
    }
  }

}
