package org.folio.dew.service;

import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;

import java.util.HashMap;
import java.util.List;

public class FolioExecutionContextManager {

  public static final String X_OKAPI_TENANT = "x-okapi-tenant";
  public DefaultFolioExecutionContext refreshAndGetFolioExecutionContext(String tenantId, FolioExecutionContext folioExecutionContext) {
    var headers = new HashMap<>(folioExecutionContext.getAllHeaders());
    headers.replace(X_OKAPI_TENANT, List.of(tenantId));
    return new DefaultFolioExecutionContext(folioExecutionContext.getFolioModuleMetadata(), headers);
  }
}
