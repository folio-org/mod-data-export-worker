package org.folio.dew.batch;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.error.FileOperationException;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
import org.folio.dew.repository.S3CompatibleStorage;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.core.io.WritableResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.folio.dew.utils.Constants.LINE_SEPARATOR;
import static org.folio.dew.utils.Constants.LINE_SEPARATOR_REPLACEMENT;

@Slf4j
public class AbstractStorageStreamWriter<T, S extends S3CompatibleStorage> implements ItemWriter<T> {

  private WritableResource resource;
  private S storage;
  private LineAggregator<T> lineAggregator;

  public AbstractStorageStreamWriter(String tempOutputFilePath, LocalFilesStorage localFilesStorage) {
    // TODO Should be implemented for MarcWriter
  }

  public AbstractStorageStreamWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor, S storage) {
    if (StringUtils.isBlank(tempOutputFilePath)) {
      throw new IllegalArgumentException("tempOutputFilePath is blank");
    }

    this.storage = storage;

    BeanWrapperFieldExtractor<T> fieldExtractor = new CsvFieldExtractor<>(fieldProcessor);

    fieldExtractor.setNames(extractedFieldNames);

    DelimitedLineAggregator<T> aggregator = new DelimitedLineAggregator<>();
    aggregator.setDelimiter(",");
    aggregator.setFieldExtractor(fieldExtractor);

    setLineAggregator(aggregator);

    if (StringUtils.isNotBlank(columnHeaders)) {
      try {
        storage.write(tempOutputFilePath, (columnHeaders + '\n').getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new FileOperationException(e);
      }
    }

    setResource(new S3CompatibleResource<>(tempOutputFilePath, storage));

    log.info("Creating file {}.", tempOutputFilePath);
  }

  public WritableResource getResource() {
    return resource;
  }

  public void setResource(S3CompatibleResource<S> resource) {
    this.resource = resource;
  }

  public void setLineAggregator(LineAggregator<T> lineAggregator) {
    this.lineAggregator = lineAggregator;
  }

  public LineAggregator<T> getLineAggregator() {
    return lineAggregator;
  }

  public S3CompatibleStorage getStorage() {
    return storage;
  }

  @Override
  public void write(Chunk<? extends T> items) throws Exception {
    var sb = new StringBuilder();
    for (T item : items) {
      sb.append(lineAggregator.aggregate(item)).append('\n');
    }
    storage.append(resource.getFilename(), sb.toString().getBytes(StandardCharsets.UTF_8));
  }
}
