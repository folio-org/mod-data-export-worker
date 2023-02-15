package org.folio.dew.batch.eholdings;

import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_MAX_TITLE_NOTES_COUNT;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_TOTAL_RESOURCES;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.folio.dew.repository.EHoldingsResourceRepository;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component("GetEHoldingsWriter")
@StepScope
public class GetEHoldingsWriter implements ItemWriter<EHoldingsResourceDTO> {

  private Long jobId;
  private JobExecution jobExecution;
  private ExecutionContext stepExecutionContext;
  private final EHoldingsResourceRepository repository;

  public GetEHoldingsWriter(EHoldingsResourceRepository repository) {
    this.repository = repository;
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    jobId = stepExecution.getJobExecutionId();
    jobExecution = stepExecution.getJobExecution();
    stepExecutionContext = stepExecution.getExecutionContext();
  }

  @Override
  public void write(Chunk<? extends EHoldingsResourceDTO> list) throws Exception {
    var resources = list.getItems().stream().map(EHoldingsResourceMapper::convertToEntity).collect(Collectors.toList());
    resources.forEach(r -> r.setJobExecutionId(jobId));
    repository.saveAll(resources);
    jobExecution.getExecutionContext().putInt(CONTEXT_TOTAL_RESOURCES,
      jobExecution.getExecutionContext().getInt(CONTEXT_TOTAL_RESOURCES, 0) + resources.size());

    var resourceWithMaxNotes = list.getItems().stream()
      .max(Comparator.comparing(p -> p.getNotes().size()))
      .orElse(null);
    var noteCollectionSize = resourceWithMaxNotes == null ? 0 : resourceWithMaxNotes.getNotes().size();

    if (noteCollectionSize > 0) {
      var resourceMaxNotesCount =
        stepExecutionContext.getInt(CONTEXT_MAX_TITLE_NOTES_COUNT, 0);
      if (resourceMaxNotesCount < noteCollectionSize) {
        stepExecutionContext.putInt(CONTEXT_MAX_TITLE_NOTES_COUNT, noteCollectionSize);
      }
    }
  }
}
