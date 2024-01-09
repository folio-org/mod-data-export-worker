package org.folio.dew.batch.bulkedit.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.InstanceCollection;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.error.BulkEditException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditInstanceListProcessor implements ItemProcessor<InstanceCollection, List<InstanceFormat>> {
  private final BulkEditInstanceProcessor bulkEditInstanceProcessor;

  @Override
  public List<InstanceFormat> process(InstanceCollection instances) {
    if (instances.getInstances().isEmpty()) {
      log.error(NO_MATCH_FOUND_MESSAGE);
      throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
    }
    return instances.getInstances().stream()
      .map(bulkEditInstanceProcessor::process)
      .toList();
  }
}
