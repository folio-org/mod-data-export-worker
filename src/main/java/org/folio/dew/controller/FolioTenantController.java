package org.folio.dew.controller;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.service.BulkEditConfigurationService;
import org.folio.spring.controller.TenantController;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("folioTenantController")
@RequestMapping
@Log4j2
public class FolioTenantController extends TenantController {

  private final KafkaService kafka;
  private final BulkEditConfigurationService configurationService;

  public FolioTenantController(TenantService baseTenantService, KafkaService kafka, BulkEditConfigurationService configurationService) {
    super(baseTenantService);
    this.kafka = kafka;
    this.configurationService = configurationService;
  }

  @Override
  public ResponseEntity<Void> postTenant(TenantAttributes tenantAttributes) {
    var tenantInit = super.postTenant(tenantAttributes);

    if (tenantInit.getStatusCode() == HttpStatus.OK || tenantInit.getStatusCode() == HttpStatus.NO_CONTENT) {
      try {
        kafka.createKafkaTopics();
        kafka.restartEventListeners();
        configurationService.checkBulkEditConfiguration();
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.noContent().build();
      }
    }

    return tenantInit;
  }

  @Override
  public ResponseEntity<Void> deleteTenant(String operationId) {
    return ResponseEntity.noContent().build();
  }

  @Override
  public void disableTenant() {
    log.info("Disabling tenant");
  }
}
