package org.folio.dew.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.spring.FolioExecutionContext;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FolioTenantServiceTest {

  @Mock
  private KafkaService kafkaService;
  @Mock
  private UserTenantsService userTenantsService;
  @Mock
  private FolioExecutionContext context;
  @InjectMocks
  private FolioTenantService folioTenantService;

  @Test
  void testIsConsortiumTenant_ShouldReturnTrue_ForConsortiumTenant() {
    var tenantId = "consortium";
    when(context.getTenantId()).thenReturn(tenantId);
    when(userTenantsService.getCentralTenant(tenantId)).thenReturn(Optional.of("centralTenant"));

    var result = folioTenantService.isConsortiumTenant();

    assertTrue(result);
    verify(userTenantsService).getCentralTenant(tenantId);
  }

  @Test
  void testIsConsortiumTenant_ShouldReturnFalse_ForNonConsortiumTenant() {
    var tenantId = "test_tenant";
    when(context.getTenantId()).thenReturn(tenantId);
    when(userTenantsService.getCentralTenant(tenantId)).thenReturn(Optional.empty());

    var result = folioTenantService.isConsortiumTenant();

    assertFalse(result);
    verify(userTenantsService).getCentralTenant(tenantId);
  }

  @Test
  void testAfterTenantUpdate_Positive() {
    folioTenantService.afterTenantUpdate(new TenantAttributes());

    verify(kafkaService).createKafkaTopics();
    verify(kafkaService).restartEventListeners();
  }

  @Test
  void testAfterTenantUpdate_Negative_ExceptionThrown() {
    doThrow(new RuntimeException("Kafka error")).when(kafkaService).createKafkaTopics();

    folioTenantService.afterTenantUpdate(new TenantAttributes());

    verify(kafkaService).createKafkaTopics();
    verify(kafkaService, never()).restartEventListeners();
  }
}
