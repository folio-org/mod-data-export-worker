package org.folio.dew.batch.bulkedit.jobs.updatejob;

import java.util.List;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;
import org.springframework.batch.item.ItemWriter;

public class BulkEditUpdateUserRecordsWriter implements ItemWriter<User> {

  private UserClient userClient;

  @Override
  public void write(List<? extends User> items) throws Exception {
    items.forEach(user -> {
      userClient.updateUser(user, user.getId());
    });
  }
}
