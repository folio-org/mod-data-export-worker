package org.folio.dew.batch.bulkedit.jobs;

import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionsValidator;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.User;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.ConsortiaService;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TenantResolverTest {

  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private PermissionsValidator permissionsValidator;
  @Mock
  private BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  @Mock
  private UserClient userClient;
  @Mock
  private JobExecution jobExecution;

  @InjectMocks
  private TenantResolver tenantResolver;

  @Test
  void testGetAffiliatedPermittedTenantIdsRecords() {
    var user = new User();
    user.setUsername("userName");
    var tenantsIds = Set.of("member1", "member2", "member3");

    var hrid  = "hrid";
    var itemIdentifier = new ItemIdentifier(hrid);

    var builder = new JobParametersBuilder();
    var jobId = "jobId";
    builder.addString(JobParameterNames.JOB_ID, jobId);
    var fileName = "fileName";
    builder.addString(FILE_NAME, fileName);
    var jobParameters = builder.toJobParameters();

    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(userClient.getUserById(any())).thenReturn(user);
    when(consortiaService.getAffiliatedTenants(any(), any())).thenReturn(List.of("member2", "member3"));
    when(permissionsValidator.isBulkEditReadPermissionExists("member2", EntityType.HOLDINGS_RECORD)).thenReturn(false);
    when(permissionsValidator.isBulkEditReadPermissionExists("member3", EntityType.HOLDINGS_RECORD)).thenReturn(true);
    when(jobExecution.getJobParameters()).thenReturn(jobParameters);

    var affiliatedAndPermittedTenants = tenantResolver.getAffiliatedPermittedTenantIds(EntityType.HOLDINGS_RECORD, jobExecution, "HRID", tenantsIds, itemIdentifier);

    assertEquals(1, affiliatedAndPermittedTenants.size());
    verify(bulkEditProcessingErrorsService, times(2)).saveErrorInCSV(eq(jobId), eq(itemIdentifier.getItemId()), isA(BulkEditException.class), eq(fileName));
  }
}
