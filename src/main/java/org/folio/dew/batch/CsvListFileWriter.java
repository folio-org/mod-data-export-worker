package org.folio.dew.batch;

import static java.util.Objects.nonNull;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.SrsClient;
import org.folio.dew.domain.dto.Formatable;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

@Log4j2
public class CsvListFileWriter<T, U extends Formatable<T>> extends FlatFileItemWriter<List<U>> {
  private CsvFileWriter<T, U> delegate;
  private MarcAsStringWriter marcDelegate;
  private SrsClient srsClient;

  public CsvListFileWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor) {
    delegate = new CsvFileWriter<>(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor);
    setResource(new FileSystemResource(tempOutputFilePath));
  }

  public CsvListFileWriter(String tempOutputFilePath, String tempOutputMarcPath, SrsClient srsClient, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor) {
    this(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor);
    this.srsClient = srsClient;
    this.marcDelegate = new MarcAsStringWriter(tempOutputMarcPath);
  }

  @Override
  public void write(Chunk<? extends List<U>> items) throws Exception {
    delegate.write(new Chunk<>(items.getItems().stream().flatMap(List::stream).toList()));
    if (nonNull(marcDelegate)) {
      marcDelegate.write(new Chunk<List<String>>(items.getItems().stream().flatMap(List::stream)
        .filter(itm -> itm.isInstanceFormat() && itm.isSourceMarc()).map(marc -> getMarcContent(marc.getId()))
        .filter(Objects::nonNull).toList()));
    }
  }

  @Override
  public void afterPropertiesSet() {
    Assert.notNull(delegate, "Delegate was not set");
  }

  @Override
  public void open(ExecutionContext executionContext) {
    if (nonNull(delegate)) {
      ((ItemStream) delegate).open(executionContext);
    }
    if (nonNull(marcDelegate)) {
      ((ItemStream) marcDelegate).open(executionContext);
    }
  }

  @Override
  public void update(ExecutionContext executionContext) {
    if (nonNull(delegate)) {
      ((ItemStream) delegate).update(executionContext);
    }
    if (nonNull(marcDelegate)) {
      ((ItemStream) marcDelegate).update(executionContext);
    }
  }

  @Override
  public void close() {
    if (nonNull(delegate)) {
      ((ItemStream) delegate).close();
    }
    if (nonNull(marcDelegate)) {
      ((ItemStream) marcDelegate).close();
    }
  }

  private String getMarcContent(String id) {
    var srsRecords = srsClient.getMarc(id, "INSTANCE");
    if (srsRecords.getSourceRecords().isEmpty()) {
      log.warn("No SRS records found by instanceId = {}", id);
      return null;
    }
    var recordId = srsRecords.getSourceRecords().get(0).getRecordId();
    var marcRecord = srsClient.getMarcContent(recordId);
    log.info("MARC record found by recordId = {}", recordId);
    return marcRecord.getRawRecord().getContent();
  }
}
