package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;

import java.util.List;

public class BulkEditCsvItemReader extends CsvItemReader<User> {

  private final UserClient userClient;
  private final String query;

  public BulkEditCsvItemReader(UserClient userClient, String query, Long offset, Long limit) {
    super(offset, limit);

    this.userClient = userClient;
    this.query = query;
  }

  @Override
  protected List<User> getItems(int offset, int limit) {
    return userClient.getUserByQuery(query, limit).getUsers();
  }

}
