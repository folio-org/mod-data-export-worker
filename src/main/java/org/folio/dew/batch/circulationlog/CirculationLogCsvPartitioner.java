package org.folio.dew.batch.circulationlog;

import org.folio.dew.batch.CsvPartitioner;
import org.folio.dew.client.AuditClient;

public class CirculationLogCsvPartitioner extends CsvPartitioner {

  private final AuditClient auditClient;
  private final String query;

  public CirculationLogCsvPartitioner(Long offset, Long limit, String tempOutputFilePath, AuditClient auditClient, String query) {
    super(offset, limit, tempOutputFilePath);
    this.auditClient = auditClient;
    this.query = query;
  }

  @Override
  protected long getLimit() {
    return auditClient.getCirculationAuditLogs(query, 0, 1, null).getTotalRecords();
  }

}
