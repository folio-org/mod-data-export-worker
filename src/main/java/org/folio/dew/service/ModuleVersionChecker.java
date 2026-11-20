package org.folio.dew.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Warns on startup if the Kafka tenant filter is enabled but MODULE_VERSION is not set. */
@Log4j2
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "folio.kafka.tenant-filter", name = "enabled", havingValue = "true")
public class ModuleVersionChecker {

  static final String MODULE_VERSION = "MODULE_VERSION";

  private final Environment environment;

  @PostConstruct
  void check() {
    if (!isModuleVersionSet()) {
      log.warn(
          "Kafka tenant filter is enabled, but {} is not set. The version without the CI build "
              + "number will be used, which may cause entitlement lookup to fail",
          MODULE_VERSION);
    }
  }

  private boolean isModuleVersionSet() {
    return StringUtils.isNotBlank(environment.getProperty(MODULE_VERSION));
  }
}
