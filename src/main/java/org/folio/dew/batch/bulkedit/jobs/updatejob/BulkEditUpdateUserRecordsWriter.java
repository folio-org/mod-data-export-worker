package org.folio.dew.batch.bulkedit.jobs.updatejob;

import java.util.List;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@Qualifier("updateUserRecordsWriter")
@RequiredArgsConstructor
public class BulkEditUpdateUserRecordsWriter implements ItemWriter<User> {

  private final UserClient userClient;

  @Override
  public void write(List<? extends User> items) throws Exception {
    items.forEach(user -> {
      try {
        userClient.updateUser(user, user.getId());
      } catch (Exception e) {
        log.debug("Cannot update user with id '%s'. Reason: " + e.getMessage());
      }
    });
  }

}
