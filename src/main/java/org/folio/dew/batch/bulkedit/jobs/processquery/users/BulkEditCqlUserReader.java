package org.folio.dew.batch.bulkedit.jobs.processquery.users;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;

import java.util.List;

public class BulkEditCqlUserReader extends CsvItemReader<User> {

  private static final int QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST = 100;

  private final UserClient userClient;
  private final String query;

  public BulkEditCqlUserReader(UserClient userClient, String query, Long offset, Long limit) {
    super(offset, limit, QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST);

    this.userClient = userClient;
    this.query = query;
  }

  @Override
  protected List<User> getItems(int offset, int limit) {
    return userClient.getUserByQuery(query, offset, limit).getUsers();
  }

}
