package org.folio.dew.batch.bulkedit.jobs.updatejob;

import java.util.List;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;
import org.folio.spring.exception.NotFoundException;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@Qualifier("updateUserRecordsWriter")
@JobScope
@RequiredArgsConstructor
public class BulkEditUpdateUserRecordsWriter implements ItemWriter<User> {

  private static final int BATCH_SIZE = 10;

  private final UserClient userClient;

  @Value("#{jobExecution}")
  private JobExecution jobExecution;

  @Override
  public void write(List<? extends User> items) throws Exception {
    items.forEach(user -> {
      try {
        userClient.updateUser(user, user.getId());
      } catch (NotFoundException e) {
        log.debug(String.format("Cannot update user. User with id '%s' wasn't found", user.getId()));
      } catch (Exception e) {
        log.debug("Cannot update user with id '%s'. Reason: " + e.getMessage());
      }
    });
  }

}
