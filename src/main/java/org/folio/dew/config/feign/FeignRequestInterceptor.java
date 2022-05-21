package org.folio.dew.config.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.collections.map.HashedMap;
import org.folio.dew.service.JobCommandsReceiverService;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class FeignRequestInterceptor implements RequestInterceptor {

  @Autowired
  private JobCommandsReceiverService jobCommandsReceiverService;

  @Autowired
  private FolioExecutionContext folioExecutionContext;

  @Override
  public void apply(RequestTemplate requestTemplate) {
    Map<String, Collection<String>> okapiHeaders = new HashedMap();
    okapiHeaders.put(TENANT, List.of(jobCommandsReceiverService.getTenantId()));
    var defaultFolioExecutionContext = new DefaultFolioExecutionContext(folioExecutionContext.getFolioModuleMetadata(), okapiHeaders);
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(defaultFolioExecutionContext);
  }
}
