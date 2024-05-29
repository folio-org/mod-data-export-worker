package org.folio.dew.batch;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.SrsClient;
import org.folio.dew.domain.dto.Formatable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.nonNull;

@Log4j2
@StepScope
public class MarcAsListStringsWriter<T, U extends Formatable<T>> extends FlatFileItemWriter<List<U>> {

  private SrsClient srsClient;
  private MarcAsStringWriter<String> delegateToStringWriter;

  public MarcAsListStringsWriter(String outputFileName, SrsClient srsClient) {
    super();
    this.srsClient = srsClient;
    delegateToStringWriter = new MarcAsStringWriter<>(outputFileName);
  }

  @Override
  public void write(Chunk<? extends List<U>> items) throws Exception {
    delegateToStringWriter.write(new Chunk<>(items.getItems().stream().flatMap(List::stream).filter(itm -> itm.isInstanceFormat() && itm.isSourceMarc()).map(marc -> getMarcContent(marc.getId()))
      .filter(Objects::nonNull).toList()));
  }

  @Override
  public void afterPropertiesSet() {
    Assert.notNull(delegateToStringWriter, "Delegate was not set");
  }

  @Override
  public void open(ExecutionContext executionContext) {
    if (nonNull(delegateToStringWriter)) {
      ((ItemStream) delegateToStringWriter).open(executionContext);
    }
  }

  @Override
  public void update(ExecutionContext executionContext) {
    if (nonNull(delegateToStringWriter)) {
      ((ItemStream) delegateToStringWriter).update(executionContext);
    }
  }

  @Override
  public void close() {
    if (nonNull(delegateToStringWriter)) {
      ((ItemStream) delegateToStringWriter).close();
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
