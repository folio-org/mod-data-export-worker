package org.folio.dew.batch.bulkedit.jobs;

import static java.lang.String.format;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.NO_HOLDING_AFFILIATION;
import static org.folio.dew.utils.Constants.NO_HOLDING_VIEW_PERMISSIONS;
import static org.folio.dew.utils.Constants.NO_ITEM_AFFILIATION;
import static org.folio.dew.utils.Constants.NO_ITEM_VIEW_PERMISSIONS;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionsValidator;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TenantResolver {

  private final FolioExecutionContext folioExecutionContext;
  private final ConsortiaService consortiaService;
  private final PermissionsValidator permissionsValidator;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  private final UserClient userClient;

  public Set<String> getAffiliatedPermittedTenantIds(EntityType entityType, JobExecution jobExecution, String identifierType, Set<String> tenantIds, ItemIdentifier itemIdentifier) {
    var affiliatedTenants = consortiaService.getAffiliatedTenants(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
    var jobId = jobExecution.getJobParameters().getString(JobParameterNames.JOB_ID);
    var fileName = FilenameUtils.getName(jobExecution.getJobParameters().getString(FILE_NAME));
    var affiliatedAndPermittedTenants = new HashSet<String>();
    for (var tenantId : tenantIds) {
      if (!affiliatedTenants.contains(tenantId)) {
        var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
        var bulkEditException = new BulkEditException(format(getAffiliationErrorPlaceholder(entityType), user.getUsername(),
          resolveIdentifier(identifierType), itemIdentifier.getItemId(), tenantId));
        bulkEditProcessingErrorsService.saveErrorInCSV(jobId, itemIdentifier.getItemId(), bulkEditException, fileName);
      } else if (!permissionsValidator.isBulkEditReadPermissionExists(tenantId, entityType)) {
        var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
        var bulkEditException = new BulkEditException(format(getViewPermissionErrorPlaceholder(entityType), user.getUsername(),
          resolveIdentifier(identifierType), itemIdentifier.getItemId(), tenantId));
        bulkEditProcessingErrorsService.saveErrorInCSV(jobId, itemIdentifier.getItemId(), bulkEditException, fileName);
      } else {
        affiliatedAndPermittedTenants.add(tenantId);
      }
    }
    return affiliatedAndPermittedTenants;
  }

  private String getAffiliationErrorPlaceholder(EntityType entityType) {
    if (entityType == EntityType.ITEM) {
      return NO_ITEM_AFFILIATION;
    }
    return NO_HOLDING_AFFILIATION;
  }

  private String getViewPermissionErrorPlaceholder(EntityType entityType) {
    if (entityType == EntityType.ITEM) {
      return NO_ITEM_VIEW_PERMISSIONS;
    }
    return NO_HOLDING_VIEW_PERMISSIONS;
  }
}