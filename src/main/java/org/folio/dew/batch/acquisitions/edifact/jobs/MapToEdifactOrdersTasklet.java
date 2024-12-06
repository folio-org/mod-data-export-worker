package org.folio.dew.batch.acquisitions.edifact.jobs;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.dew.batch.acquisitions.edifact.PurchaseOrdersToEdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.client.DataExportSpringClient;
import org.folio.dew.domain.dto.EdiConfig;
import org.folio.dew.domain.dto.ExportConfig;
import org.folio.dew.domain.dto.ExportConfigCollection;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.acquisitions.edifact.EdifactExportHolder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Component
@StepScope
@Log4j2
public class MapToEdifactOrdersTasklet extends MapToEdifactTasklet {

  private final DataExportSpringClient dataExportSpringClient;

  public MapToEdifactOrdersTasklet(ObjectMapper ediObjectMapper, OrdersService ordersService,
                                   DataExportSpringClient dataExportSpringClient,
                                   PurchaseOrdersToEdifactMapper purchaseOrdersToEdifactMapper) {
    super(ediObjectMapper, ordersService, purchaseOrdersToEdifactMapper);
    this.dataExportSpringClient = dataExportSpringClient;
  }

  protected List<String> getExportConfigMissingFields(VendorEdiOrdersExportConfig ediOrdersExportConfig) {
    return List.of();
  }

  @Override
  protected EdifactExportHolder buildEdifactExportHolder(ChunkContext chunkContext, VendorEdiOrdersExportConfig ediExportConfig, Map<String, Object> jobParameters) {
    var poLineQuery = getPoLineQuery(ediExportConfig);
    var compOrders = getCompositeOrders(poLineQuery);
    return new EdifactExportHolder(compOrders, List.of());
  }

  protected String getPoLineQuery(VendorEdiOrdersExportConfig ediConfig) {
    // Order filters
    var workflowStatusFilter = "purchaseOrder.workflowStatus==Open"; // order status is Open
    var vendorFilter = String.format(" AND purchaseOrder.vendor==%s", ediConfig.getVendorId()); // vendor id matches
    var notManualFilter = " AND (cql.allRecords=1 NOT purchaseOrder.manualPo==true)"; // not a manual order

    // Order line filters
    var automaticExportFilter = " AND automaticExport==true"; // line with automatic export
    var ediExportDateFilter = " AND (cql.allRecords=1 NOT lastEDIExportDate=\"\")"; // has not been exported yet
    var acqMethodsFilter = fieldInListFilter("acquisitionMethod",
      ediConfig.getEdiConfig().getDefaultAcquisitionMethods()); // acquisitionMethod in default list
    String vendorAccountFilter = "";
    if (Boolean.TRUE.equals(ediConfig.getIsDefaultConfig())) {
      var configQuery = String.format("configName==%s_%s*", ExportType.EDIFACT_ORDERS_EXPORT, ediConfig.getVendorId());
      var configs = dataExportSpringClient.getExportConfigs(configQuery);
      if (configs.getTotalRecords() > 1) {
        var accountNoSetForExclude = getAccountNoSet(configs);
        vendorAccountFilter = fieldNotInListFilter("vendorDetail.vendorAccount", accountNoSetForExclude);
      }
    } else {
      // vendorAccount in the config account number list
      vendorAccountFilter = fieldInListFilter("vendorDetail.vendorAccount",
        ediConfig.getEdiConfig().getAccountNoList());
    }

    var resultQuery = String.format("%s%s%s%s%s%s%s", workflowStatusFilter, vendorFilter, notManualFilter,
      automaticExportFilter, ediExportDateFilter, acqMethodsFilter, vendorAccountFilter);
    log.info("GET purchase order line query: {}", resultQuery);
    return resultQuery;
  }

  private Set<String> getAccountNoSet(ExportConfigCollection configs) {
    Set<String> accountNoSet = new HashSet<>();
    for (ExportConfig exportConfig : configs.getConfigs()) {
      EdiConfig ediConfig = exportConfig.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig().getEdiConfig();
      if (Objects.nonNull(ediConfig)) {
        List<String> currentAccountNoList = ediConfig.getAccountNoList();
        if (CollectionUtils.isNotEmpty(currentAccountNoList)) {
          accountNoSet.addAll(currentAccountNoList);
        }
      }
    }
    return accountNoSet;
  }

  private String fieldInListFilter(String fieldName, List<?> list) {
    return String.format(" AND %s==%s", fieldName,
      list.stream()
        .map(item -> String.format("\"%s\"", item.toString()))
        .collect(Collectors.joining(" OR ", "(", ")")));
  }

  private static String fieldNotInListFilter(String fieldName, Collection<?> list) {
    return String.format(" AND cql.allRecords=1 NOT %s==%s", fieldName,
      list.stream()
        .map(item -> String.format("\"%s\"", item.toString()))
        .collect(Collectors.joining(" OR ", "(", ")")));
  }

}
