package org.folio.dew.batch.bulkedit.jobs;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ExtendedItemCollection;
import org.folio.dew.domain.dto.ItemFormat;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemListProcessor implements ItemProcessor<ExtendedItemCollection, List<ItemFormat>> {
  private final BulkEditItemProcessor bulkEditItemProcessor;

  @Override
  public List<ItemFormat> process(ExtendedItemCollection extendedItemCollection) {
    return extendedItemCollection.getExtendedItems().stream()
      .map(bulkEditItemProcessor::process)
      .toList();
  }
}
