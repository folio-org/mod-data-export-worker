package org.folio.dew.batch.circulationlog;

import org.folio.dew.batch.CsvPartitioner;
import org.folio.dew.client.AuditClient;
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

public class CirculationLogCsvPartitioner extends CsvPartitioner {

  private final AuditClient auditClient;
  private final String query;

  @Autowired
  private FolioExecutionContext folioExecutionContext;

  @Value("#{jobParameters['tenantId']}")
  private String tenantId;

  public CirculationLogCsvPartitioner(Long offset, Long limit, String tempOutputFilePath, AuditClient auditClient, String query) {
    super(offset, limit, tempOutputFilePath);

    this.auditClient = auditClient;
    this.query = query;
  }

  @Override
  protected Long getLimit() {
    Map<String, Collection<String>> okapiHeaders = new HashMap(folioExecutionContext.getOkapiHeaders());
    okapiHeaders.put(TENANT, List.of(tenantId));
    var defaultFolioExecutionContext = new DefaultFolioExecutionContext(folioExecutionContext.getFolioModuleMetadata(), okapiHeaders);
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(defaultFolioExecutionContext);
    return Long.valueOf(auditClient.getCirculationAuditLogs(query, 0, 1, null).getTotalRecords());
  }

}
