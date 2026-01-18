package org.folio.dew.batch;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.SrsClient;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.domain.dto.Formatable;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.JsonToMarcConverter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.folio.dew.utils.Constants.NO_MARC_CONTENT;

@Log4j2
@StepScope
public class MarcAsListStringsWriter<T, U extends Formatable<T>> extends FlatFileItemWriter<List<U>> {

  private SrsClient srsClient;
  private MarcAsStringWriter<String> delegateToStringWriter;
  private JsonToMarcConverter jsonToMarcConverter;

  public MarcAsListStringsWriter(String outputFileName, SrsClient srsClient, JsonToMarcConverter jsonToMarcConverter) {
    super();
    this.srsClient = srsClient;
    this.jsonToMarcConverter = jsonToMarcConverter;
    delegateToStringWriter = new MarcAsStringWriter<>(outputFileName);
  }

  @Override
  public void write(Chunk<? extends List<U>> items) throws Exception {
    delegateToStringWriter.write(new Chunk<>(items.getItems().stream().flatMap(List::stream)
      .filter(itm -> itm.isInstanceFormat() && itm.isSourceMarc()).map(marc -> {
        try {
          return getMarcContent(marc.getId());
        } catch (Exception e) {
          log.error(e);
          throw new BulkEditException(format(NO_MARC_CONTENT, marc.getId(), e.getMessage()), ErrorType.ERROR);
        }
      })
      .flatMap(List::stream).filter(Objects::nonNull).toList()));
  }

  @Override
  public void afterPropertiesSet() {
    Assert.notNull(delegateToStringWriter, "Delegate was not set");
  }

  @Override
  public void open(ExecutionContext executionContext) {
    if (nonNull(delegateToStringWriter)) {
      delegateToStringWriter.open(executionContext);
    }
  }

  @Override
  public void update(ExecutionContext executionContext) {
    if (nonNull(delegateToStringWriter)) {
      delegateToStringWriter.update(executionContext);
    }
  }

  @Override
  public void close() {
    if (nonNull(delegateToStringWriter)) {
      delegateToStringWriter.close();
    }
  }

  private List<String> getMarcContent(String id) throws Exception {
    List<String> mrcRecords = new ArrayList<>();
    var srsRecords = srsClient.getMarc(id, "INSTANCE", true).get("sourceRecords");
    if (srsRecords.isEmpty()) {
      log.warn("No SRS records found by instanceId = {}", id);
      return mrcRecords;
    }
    for (var jsonNodeIterator = srsRecords.elements(); jsonNodeIterator.hasNext();) {
      var srsRec = jsonNodeIterator.next();
      var parsedRec = srsRec.get("parsedRecord");
      var content = parsedRec.get("content").toString();
      mrcRecords.add(jsonToMarcConverter.convertJsonRecordToMarcRecord(content));
    }
    return mrcRecords;
  }
}
