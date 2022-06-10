package org.folio.dew.batch.bulkedit.jobs;

import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ItemCollection;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.error.BulkEditException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditItemListProcessor implements ItemProcessor<ItemCollection, List<ItemFormat>> {
  private final BulkEditItemProcessor bulkEditItemProcessor;

  @Override
  public List<ItemFormat> process(ItemCollection items) {
    if (items.getItems().isEmpty()) {
      log.error(NO_MATCH_FOUND_MESSAGE);
      throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
    }
    return items.getItems().stream()
      .map(bulkEditItemProcessor::process)
      .collect(Collectors.toList());
  }
}
