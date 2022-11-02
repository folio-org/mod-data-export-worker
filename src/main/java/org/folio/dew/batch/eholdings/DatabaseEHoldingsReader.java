package org.folio.dew.batch.eholdings;

import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_TOTAL_RESOURCES;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.EHoldingsResource;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.folio.dew.repository.EHoldingsResourceRepository;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class DatabaseEHoldingsReader extends AbstractEHoldingsReader<EHoldingsResourceDTO> {
  private Long jobExecutionId;
  private static int quantityToRetrievePerRequest = 100;
  private final EHoldingsResourceRepository resourceRepository;

  private int totalResources;

  public static void setQuantityToRetrievePerRequest(int quantityToRetrievePerRequest){
    DatabaseEHoldingsReader.quantityToRetrievePerRequest = quantityToRetrievePerRequest;
  }

  protected DatabaseEHoldingsReader(EHoldingsResourceRepository resourceRepository) {
    super(null, 1L, quantityToRetrievePerRequest);
    this.resourceRepository = resourceRepository;
  }

  @BeforeStep
  private void beforeStep(StepExecution stepExecution) {
    jobExecutionId = stepExecution.getJobExecutionId();

    var executionContext = stepExecution.getJobExecution().getExecutionContext();
    totalResources = executionContext.getInt(CONTEXT_TOTAL_RESOURCES, 1);
  }

  @Override
  protected List<EHoldingsResourceDTO> getItems(EHoldingsResourceDTO last, int limit) {
    List<EHoldingsResource> eHoldingsResources;
    var resourceName = StringUtils.EMPTY;
    var resourceId = StringUtils.EMPTY;
    if (last != null) {
      var resourceAttributes = last.getResourcesData().getAttributes();
      resourceName = resourceAttributes.getName().toLowerCase();
      resourceId = resourceAttributes.getPackageId() + '-' + resourceAttributes.getTitleId();
    }

    eHoldingsResources = resourceRepository.seek(resourceName, resourceId, jobExecutionId, limit);
    return EHoldingsResourceMapper.convertToDTO(eHoldingsResources);
  }

  @Override
  protected void doOpen() {
    setMaxItemCount(totalResources);
  }

  @Override
  protected void doClose() {
    //Nothing to do
  }
}
