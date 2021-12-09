package org.folio.dew.batch.bulkedit.jobs.rollbackjob;

import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.bulkedit.jobs.updatejob.BulkEditUpdateUserRecordsProcessor;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.service.BulkEditParseService;
import org.folio.dew.service.BulkEditRollBackService;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@JobScope
@RequiredArgsConstructor
public class BulkEditFilterUserRecordsForRollBackProcessor extends BulkEditUpdateUserRecordsProcessor {

  @Value("#{jobParameters['jobId']}")
  private String jobId;
  private final BulkEditRollBackService bulkEditRollBackService;
  private BulkEditParseService bulkEditParseService;

  @Override
  public User process(UserFormat userFormat) {
    if (bulkEditRollBackService.isUserIdExistForJob(userFormat.getId(), UUID.fromString(jobId))) {
      return bulkEditParseService.mapUserFormatToUser(userFormat);
    }
    return null;
  }
}
