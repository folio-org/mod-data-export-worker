package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.utils.QueryUtils.combineCqlExpressions;
import static org.folio.dew.utils.QueryUtils.convertFieldListToCqlQuery;
import static org.folio.dew.utils.QueryUtils.getCqlExpressionForFieldNullValue;
import static org.folio.dew.utils.QueryUtils.negateQuery;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.dew.batch.acquisitions.edifact.PurchaseOrdersToEdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.client.DataExportSpringClient;
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
    var acqMethods = ediConfig.getEdiConfig().getDefaultAcquisitionMethods();
    var resultQuery = combineCqlExpressions("AND",
      // Order filters
      "purchaseOrder.workflowStatus==Open", // order status is Open
      "purchaseOrder.vendor==%s".formatted(ediConfig.getVendorId()), // vendor id matches
      negateQuery("purchaseOrder.manualPo==true"), // not a manual order

      // Order line filters
      "automaticExport==true", // line with automatic export
      getCqlExpressionForFieldNullValue("lastEDIExportDate"), // has not been exported yet
      convertFieldListToCqlQuery(acqMethods, "acquisitionMethod", true), // acquisitionMethod in default list
      getVendorAccountFilter(ediConfig) // vendor account no filter
    );
    log.info("getPoLineQuery:: Fetching purchase order lines with query: {}", resultQuery);
    return resultQuery;
  }

  private String getVendorAccountFilter(VendorEdiOrdersExportConfig ediConfig) {
    if (Boolean.TRUE.equals(ediConfig.getIsDefaultConfig())) {
      var configQuery = "configName==%s_%s*".formatted(ExportType.EDIFACT_ORDERS_EXPORT, ediConfig.getVendorId());
      var configs = dataExportSpringClient.getExportConfigs(configQuery);
      return configs.getTotalRecords() > 1
        ? negateQuery(convertFieldListToCqlQuery(extractAllAccountNoLists(configs), "vendorDetail.vendorAccount", true))
        : null;
    }
    return convertFieldListToCqlQuery(ediConfig.getEdiConfig().getAccountNoList(), "vendorDetail.vendorAccount", true);
  }

  private Set<String> extractAllAccountNoLists(ExportConfigCollection configs) {
    return configs.getConfigs().stream()
      .map(config -> config.getExportTypeSpecificParameters().getVendorEdiOrdersExportConfig().getEdiConfig())
      .map(config -> Objects.nonNull(config) && CollectionUtils.isNotEmpty(config.getAccountNoList())
        ? config.getAccountNoList()
        : List.<String>of())
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

}
