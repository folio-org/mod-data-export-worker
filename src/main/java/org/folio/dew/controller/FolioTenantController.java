package org.folio.dew.controller;

import lombok.extern.log4j.Log4j2;
import org.folio.des.config.KafkaConfiguration;
import org.folio.dew.service.JobCommandsReceiverService;
import org.folio.spring.controller.TenantController;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("folioTenantController")
@RequestMapping(value = "/_/")
@Log4j2
public class FolioTenantController extends TenantController {

  private final KafkaConfiguration kafka;
  private final JobCommandsReceiverService jobCommandsReceiverService;

  public FolioTenantController(TenantService baseTenantService, KafkaConfiguration kafka,
      JobCommandsReceiverService jobCommandsReceiverService) {
    super(baseTenantService);
    this.kafka = kafka;
    this.jobCommandsReceiverService = jobCommandsReceiverService;
  }

  @Override
  public ResponseEntity<String> postTenant(TenantAttributes tenantAttributes) {
    var tenantInit = super.postTenant(tenantAttributes);

    if (tenantInit.getStatusCode() == HttpStatus.OK) {
      try {
        kafka.init(KafkaConfiguration.Topic.JOB_COMMAND, jobCommandsReceiverService);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
      }
    }

    return tenantInit;
  }

}
