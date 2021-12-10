package org.folio.dew.batch.bulkedit.jobs.processquery;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;

import java.util.List;

public class BulkEditCqlItemReader extends CsvItemReader<User> {

  private final UserClient userClient;
  private final String query;

  public BulkEditCqlItemReader(UserClient userClient, String query, Long offset, Long limit) {
    super(offset, limit);

    this.userClient = userClient;
    this.query = query;
  }

  @Override
  protected List<User> getItems(int offset, int limit) {
    return userClient.getUserByQuery(query, offset, limit).getUsers();
  }

}
