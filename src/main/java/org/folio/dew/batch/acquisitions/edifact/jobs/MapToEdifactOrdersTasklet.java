package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.LIB_EDI_CODE;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.LIB_EDI_TYPE;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.VENDOR_EDI_CODE;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.VENDOR_EDI_TYPE;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportUtils.validateField;
import static org.folio.dew.utils.QueryUtils.combineCqlExpressions;
import static org.folio.dew.utils.QueryUtils.convertFieldListToEnclosedCqlQuery;
import static org.folio.dew.utils.QueryUtils.getCqlExpressionForFieldNullValue;
import static org.folio.dew.utils.QueryUtils.negateQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.acquisitions.edifact.mapper.ExportResourceMapper;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.batch.acquisitions.edifact.services.OrganizationsService;
import org.folio.dew.client.DataExportSpringClient;
import org.folio.dew.domain.dto.ExportConfigCollection;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.acquisitions.edifact.ExportHolder;
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
  private final ExportResourceMapper edifactMapper;

  public MapToEdifactOrdersTasklet(ObjectMapper ediObjectMapper, OrganizationsService organizationsService, OrdersService ordersService,
                                   DataExportSpringClient dataExportSpringClient,
                                   ExportResourceMapper edifactMapper) {
    super(ediObjectMapper, organizationsService, ordersService);
    this.edifactMapper = edifactMapper;
    this.dataExportSpringClient = dataExportSpringClient;
  }

  @Override
  protected List<String> getExportConfigMissingFields(VendorEdiOrdersExportConfig ediOrdersExportConfig) {
    List<String> missingFields = new ArrayList<>();
    var ediConfig = ediOrdersExportConfig.getEdiConfig();
    validateField(LIB_EDI_TYPE.getName(), ediConfig.getLibEdiType(), Objects::nonNull, missingFields);
    validateField(LIB_EDI_CODE.getName(), ediConfig.getLibEdiCode(), StringUtils::isNotBlank, missingFields);
    validateField(VENDOR_EDI_TYPE.getName(), ediConfig.getVendorEdiType(), Objects::nonNull, missingFields);
    validateField(VENDOR_EDI_CODE.getName(), ediConfig.getVendorEdiCode(), StringUtils::isNotBlank, missingFields);
    return missingFields;
  }

  @Override
  protected ExportHolder buildEdifactExportHolder(ChunkContext chunkContext, VendorEdiOrdersExportConfig ediExportConfig, Map<String, Object> jobParameters) {
    var poLineQuery = getPoLineQuery(ediExportConfig);
    var compOrders = getCompositeOrders(poLineQuery);
    return new ExportHolder(compOrders, List.of());
  }

  @Override
  protected ExportResourceMapper getExportResourceMapper(VendorEdiOrdersExportConfig ediOrdersExportConfig) {
    return edifactMapper;
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
      convertFieldListToEnclosedCqlQuery(acqMethods, "acquisitionMethod", true), // acquisitionMethod in default list
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
        ? negateQuery(convertFieldListToEnclosedCqlQuery(extractAllAccountNoLists(configs), "vendorDetail.vendorAccount", true))
        : "";
    }
    return convertFieldListToEnclosedCqlQuery(ediConfig.getEdiConfig().getAccountNoList(), "vendorDetail.vendorAccount", true);
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
