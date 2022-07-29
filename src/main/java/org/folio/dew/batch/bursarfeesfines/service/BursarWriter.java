package org.folio.dew.batch.bursarfeesfines.service;


import java.util.List;

import org.folio.dew.error.FileOperationException;
import org.folio.dew.repository.LocalFilesStorage;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.core.io.Resource;

@Log4j2
public class BursarWriter<T> extends AbstractItemStreamItemWriter<T>
  implements ResourceAwareItemWriterItemStream<T> {

  protected LineAggregator<T> lineAggregator;

  private LocalFilesStorage localFilesStorage;

  private Resource resource;

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
  public void setResource(Resource resource) {
    this.resource = resource;
  }

  public void setHeader(String header) {
    this.header = header;
  }


  @Override
  public void write(List<? extends T> items) throws Exception {
    StringBuilder lines = new StringBuilder();
    for (T item : items) {
      lines.append(this.lineAggregator.aggregate(item)).append(this.lineSeparator);
    }
    localFilesStorage.append(resource.getFilename(), lines.toString().getBytes());
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    try {
      localFilesStorage.write(resource.getFilename(), (header + lineSeparator).getBytes());
    } catch (Exception e) {
      throw new FileOperationException(e);
    }
  }

}
