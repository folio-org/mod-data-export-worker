package org.folio.dew.batch.bulkedit.jobs.updatejob;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;
import org.folio.dew.service.BulkEditRollBackService;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@Qualifier("updateUserRecordsWriter")
@RequiredArgsConstructor
@JobScope
public class BulkEditUpdateUserRecordsWriter implements ItemWriter<User> {

  @Value("#{jobParameters['jobId']}")
  private String jobId;
  private final UserClient userClient;
  private final BulkEditRollBackService bulkEditRollBackService;

  @Override
  public void write(List<? extends User> items) throws Exception {
    items.forEach(user -> {
      try {
        userClient.updateUser(user, user.getId());
        log.info("Update user with id - {} by job id {}", user.getId(), jobId);
        bulkEditRollBackService.putUserIdForJob(user.getId(), UUID.fromString(jobId));
      } catch (Exception e) {
        log.debug("Cannot update user with id '%s'. Reason: " + e.getMessage());
      }
    });
  }
}
