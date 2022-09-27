package org.folio.dew.batch.eholdings;

import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_TOTAL_RESOURCES;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;

import java.util.List;

import org.folio.de.entity.EHoldingsPackage;
import org.folio.de.entity.EHoldingsResource;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceExportFormat;
import org.folio.dew.repository.EHoldingsPackageRepository;
import org.folio.dew.repository.EHoldingsResourceRepository;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class DatabaseEHoldingsReader extends AbstractEHoldingsReader<EHoldingsResourceExportFormat> {
  private static final int QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST = 20;

  private final EHoldingsPackageRepository packageRepository;
  private final EHoldingsResourceRepository resourceRepository;
  private final EHoldingsToExportFormatMapper mapper;

  private final EHoldingsExportConfig.RecordTypeEnum recordType;
  private final String recordId;

  private EHoldingsPackage eHoldingsPackage;

  private int totalResources;

  protected DatabaseEHoldingsReader(EHoldingsPackageRepository packageRepository,
                                    EHoldingsResourceRepository resourceRepository,
                                    EHoldingsToExportFormatMapper mapper,
                                    EHoldingsExportConfig exportConfig) {
    super(null, 1L, QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST);

    this.packageRepository = packageRepository;
    this.resourceRepository = resourceRepository;
    this.mapper = mapper;

    this.recordId = exportConfig.getRecordId();
    this.recordType = exportConfig.getRecordType();
  }

  @BeforeStep
  private void beforeStep(StepExecution stepExecution) {
    var packageId = recordType == PACKAGE ? recordId : recordId.split("-\\d+$")[0];
    eHoldingsPackage = packageRepository.findById(packageId).orElse(null);

    var executionContext = stepExecution.getJobExecution().getExecutionContext();
    totalResources = executionContext.getInt(CONTEXT_TOTAL_RESOURCES, 1);
  }

  @Override
  protected List<EHoldingsResourceExportFormat> getItems(EHoldingsResourceExportFormat last, int limit) {
    List<EHoldingsResource> eHoldingsResources;
    if (last != null) {
      String resourceId = last.getTitleId();
      eHoldingsResources = resourceRepository.seek(resourceId, limit);
    }
    else {
      eHoldingsResources = resourceRepository.seek(limit);
    }

    return mapper.convertToExportFormat(eHoldingsPackage, eHoldingsResources);
  }

  @Override
  protected void doOpen() {
    setMaxItemCount(totalResources);
  }

  @Override
  protected void doClose(){}
}
