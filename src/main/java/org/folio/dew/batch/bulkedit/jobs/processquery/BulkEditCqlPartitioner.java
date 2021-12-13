package org.folio.dew.batch.bulkedit.jobs.processquery;

import org.folio.dew.batch.CsvPartitioner;
import org.folio.dew.client.UserClient;

public class BulkEditCqlPartitioner extends CsvPartitioner {

  private final UserClient userClient;
  private final String query;

  public BulkEditCqlPartitioner(Long offset, Long limit, String tempOutputFilePath, UserClient userClient, String query) {
    super(offset, limit, tempOutputFilePath);

    this.userClient = userClient;
    this.query = query;
  }

  @Override
  protected Long getLimit() {
    return Long.valueOf(userClient.getUserByQuery(query, 0, 1).getTotalRecords());
  }

}
