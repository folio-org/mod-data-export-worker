package org.folio.dew.batch.circulationlog;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.AuditClient;
import org.folio.dew.domain.dto.LogRecord;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CirculationLogCsvItemReader extends CsvItemReader<LogRecord> {

  private static final int QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST = 100;

  private final AuditClient auditClient;
  private final String query;

  @Autowired
  private FolioExecutionContext folioExecutionContext;

  @Value("#{jobParameters['tenantId']}")
  private String tenantId;

  public CirculationLogCsvItemReader(AuditClient auditClient, String query, Long offset, Long limit) {
    super(offset, limit, QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST);

    this.auditClient = auditClient;
    this.query = query;
  }

  @Override
  protected List<LogRecord> getItems(int offset, int limit) {
    Map<String, Collection<String>> okapiHeaders = new HashMap(folioExecutionContext.getOkapiHeaders());
    okapiHeaders.put(TENANT, List.of(tenantId));
    var defaultFolioExecutionContext = new DefaultFolioExecutionContext(folioExecutionContext.getFolioModuleMetadata(), okapiHeaders);
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(defaultFolioExecutionContext);
    return auditClient.getCirculationAuditLogs(query, offset, limit, null).getLogRecords();
  }

}
