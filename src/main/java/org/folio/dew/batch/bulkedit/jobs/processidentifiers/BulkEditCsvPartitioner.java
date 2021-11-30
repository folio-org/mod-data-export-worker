package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import org.folio.dew.batch.CsvPartitioner;
import org.folio.dew.client.AuditClient;
import org.folio.dew.client.UserClient;

public class BulkEditCsvPartitioner extends CsvPartitioner {

  private final UserClient userClient;
  private final String query;

  public BulkEditCsvPartitioner(Long offset, Long limit, String tempOutputFilePath, UserClient userClient, String query) {
    super(offset, limit, tempOutputFilePath);

    this.userClient = userClient;
    this.query = query;
  }

  @Override
  protected Long getLimit() {
    return Long.valueOf(userClient.getUserByQuery(query, Integer.MAX_VALUE).getTotalRecords());
  }

}
