package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.UserClient;

import java.util.List;

public class BulkEditCsvItemReader extends CsvItemReader<String> {

  public BulkEditCsvItemReader(UserClient userClient, String query, Long offset, Long limit) {
    super(offset, limit);

  }

  @Override
  protected List<String> getItems(int offset, int limit) {
    //TODO
  }

}
