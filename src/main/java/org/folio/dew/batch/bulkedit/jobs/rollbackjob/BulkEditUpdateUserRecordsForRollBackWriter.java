package org.folio.dew.batch.bulkedit.jobs.rollbackjob;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@JobScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditUpdateUserRecordsForRollBackWriter implements ItemWriter<User> {

  private final UserClient userClient;
  @Value("#{jobParameters['jobId']}")
  private String jobId;

  @Override
  public void write(List<? extends User> items) throws Exception {
    items.forEach(user -> {
      try {
        userClient.updateUser(user, user.getId());
        log.info("Rollback user with id - {} from updating by job {}", user.getId(), jobId);
      } catch (Exception e) {
        log.info("Cannot rollback user with id {}. Reason: {}",  user.getId(), e.getMessage());
      }
    });
  }
}
