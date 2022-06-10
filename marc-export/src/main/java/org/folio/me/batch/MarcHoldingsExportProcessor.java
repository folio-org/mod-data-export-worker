package org.folio.me.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.marc4j.marc.Record;
import org.marc4j.marc.impl.RecordImpl;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
@Log4j2
public class MarcHoldingsExportProcessor implements ItemProcessor<ItemIdentifier, Record> {
  @Override
  public Record process(ItemIdentifier itemIdentifier) {
    // implementation for holdings
    return new RecordImpl();
  }
}
