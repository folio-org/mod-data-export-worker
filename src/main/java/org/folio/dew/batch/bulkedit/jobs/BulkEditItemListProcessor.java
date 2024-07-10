package org.folio.dew.batch.bulkedit.jobs;

import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.SearchClient;
import org.folio.dew.domain.dto.BatchIdsDto;
import org.folio.dew.domain.dto.ConsortiumItem;
import org.folio.dew.domain.dto.ExtendedItemCollection;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemCollection;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.error.BulkEditException;
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
    if (extendedItemCollection.getExtendedItems().isEmpty()) {
      log.error(NO_MATCH_FOUND_MESSAGE);
      throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
    }
    return extendedItemCollection.getExtendedItems().stream()
      .map(bulkEditItemProcessor::process)
      .toList();
  }
}
