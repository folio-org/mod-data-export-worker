package org.folio.dew.service;

import org.apache.commons.lang3.SerializationUtils;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class FolioExecutionContextManager {

  public static final String X_OKAPI_TENANT = "x-okapi-tenant";
  public DefaultFolioExecutionContext refreshAndGetFolioExecutionContext(String tenantId, FolioExecutionContext folioExecutionContext) {
    var headersCopy = SerializationUtils.clone((HashMap<String, Collection<String>>) folioExecutionContext.getAllHeaders());
    headersCopy.replace(X_OKAPI_TENANT, List.of(tenantId));
    return new DefaultFolioExecutionContext(folioExecutionContext.getFolioModuleMetadata(), headersCopy);
  }
}
