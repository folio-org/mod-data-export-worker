package org.folio.dew.batch.bulkedit.jobs.rollbackjob;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@JobScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditUpdateUserRecordsForRollBackWriter implements ItemWriter<User> {

  private final UserClient userClient;

  @Override
  public void write(List<? extends User> items) throws Exception {
    items.forEach(user -> {
      userClient.updateUser(user, user.getId());
      log.info("Roll-back user with id - {}", user.getId());
    });
  }
}
