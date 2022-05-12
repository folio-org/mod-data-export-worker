package org.folio.dew.batch.circulationlog;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.AuditClient;
import org.folio.dew.domain.dto.LogRecord;

import java.util.List;

public class CirculationLogCsvItemReader extends CsvItemReader<LogRecord> {

  private final AuditClient auditClient;
  private final String query;

  public CirculationLogCsvItemReader(AuditClient auditClient, String query, Long offset, Long limit) {
    super(offset, limit, 100);

    this.auditClient = auditClient;
    this.query = query;
  }

  @Override
  protected List<LogRecord> getItems(int offset, int limit) {
    return auditClient.getCirculationAuditLogs(query, offset, limit, null).getLogRecords();
  }

}
