package org.folio.dew.batch.eholdings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.AgreementClient;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

@Component
@JobScope
@Log4j2
public class AgreementCache {

  @SuppressWarnings("java:S3749")
  private final Map<String, List<AgreementClient.Agreement>> agreementsByResourceId = new HashMap<>();

  public void put(String resourceId, AgreementClient.Agreement agreement) {
    agreementsByResourceId.computeIfAbsent(resourceId, k -> new ArrayList<>()).add(agreement);
  }

  public List<AgreementClient.Agreement> get(String resourceId) {
    return agreementsByResourceId.getOrDefault(resourceId, Collections.emptyList());
  }

  public boolean isEmpty() {
    return agreementsByResourceId.isEmpty();
  }
}
