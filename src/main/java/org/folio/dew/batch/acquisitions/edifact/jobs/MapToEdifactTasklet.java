package org.folio.dew.batch.acquisitions.edifact.jobs;

import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.groupingBy;
import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.edifact.PurchaseOrdersToEdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.exceptions.CompositeOrderMappingException;
import org.folio.dew.batch.acquisitions.edifact.exceptions.EdifactException;
import org.folio.dew.batch.acquisitions.edifact.exceptions.OrderNotFoundException;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.PurchaseOrder;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Component
@StepScope
@Log4j2
public class MapToEdifactTasklet implements Tasklet {
  private final ObjectMapper ediObjectMapper;

  private final OrdersService ordersService;
  private final PurchaseOrdersToEdifactMapper purchaseOrdersToEdifactMapper;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    log.info("Execute MapToEdifactTasklet");
    var jobParameters = chunkContext.getStepContext().getJobParameters();
    var ediExportConfig = ediObjectMapper.readValue((String)jobParameters.get(EDIFACT_ORDERS_EXPORT), VendorEdiOrdersExportConfig.class);
    validateEdiExportConfig(ediExportConfig);

    List<CompositePurchaseOrder> compOrders = getCompPOList(ediExportConfig);
    // save poLineIds in memory
    persistPoLineIds(chunkContext, compOrders);

    String jobName = jobParameters.get(JobParameterNames.JOB_NAME).toString();
    String edifactOrderAsString = purchaseOrdersToEdifactMapper.convertOrdersToEdifact(compOrders, ediExportConfig, jobName);
    // save edifact file content in memory
    ExecutionContextUtils.addToJobExecutionContext(chunkContext.getStepContext().getStepExecution(), "edifactOrderAsString", edifactOrderAsString, "");
    return RepeatStatus.FINISHED;
  }

  private void validateEdiExportConfig(VendorEdiOrdersExportConfig ediExportConfig) {
    var ediConfig = ediExportConfig.getEdiConfig();
    Optional<Integer> port = Optional.ofNullable(ediExportConfig.getEdiFtp().getFtpPort());

    if (StringUtils.isEmpty(ediConfig.getLibEdiCode()) || ediConfig.getLibEdiType() == null
      || StringUtils.isEmpty(ediConfig.getVendorEdiCode()) || ediConfig.getVendorEdiType() == null) {
      throw new EdifactException("Export configuration is incomplete, missing library EDI code/Vendor EDI code");
    }

    if (port.isEmpty()) {
      throw new EdifactException("Export configuration is incomplete, missing FTP/SFTP Port");
    }
  }

  private List<CompositePurchaseOrder> getCompPOList(VendorEdiOrdersExportConfig ediConfig) {
    var poLineQuery = buildPoLineQuery(ediConfig);
    var poLines = ordersService.getPoLinesByQuery(poLineQuery);

    var orderIds = poLines.stream()
      .map(PoLine::getPurchaseOrderId)
      .distinct()
      .toList();
    var orders = ordersService.getPurchaseOrdersByIds(orderIds);

    var compOrders = assembleCompositeOrders(orders, poLines);

    log.debug("composite purchase orders: {}", compOrders);

    if (compOrders.isEmpty()) {
      throw new OrderNotFoundException("Orders for export not found", false);
    }
    return compOrders;
  }

  private String buildPoLineQuery(VendorEdiOrdersExportConfig ediConfig) {
    // Order filters
    var workflowStatusFilter = "purchaseOrder.workflowStatus==Open"; // order status is Open
    var vendorFilter = String.format(" AND purchaseOrder.vendor==%s", ediConfig.getVendorId()); // vendor id matches
    var notManualFilter = " AND (cql.allRecords=1 NOT purchaseOrder.manualPo==true)"; // not a manual order

    // Order line filters
    var automaticExportFilter = " AND automaticExport==true"; // line with automatic export
    var ediExportDateFilter = " AND (cql.allRecords=1 NOT lastEDIExportDate=\"\")"; // has not been exported yet
    var acqMethodsFilter = fieldInListFilter("acquisitionMethod",
      ediConfig.getEdiConfig().getDefaultAcquisitionMethods()); // acquisitionMethod in default list
    String vendorAccountFilter;
    if (ediConfig.getIsDefaultConfig() != null && ediConfig.getIsDefaultConfig()) {
      // vendorAccount empty or undefined
      vendorAccountFilter = " AND (vendorDetail.vendorAccount==\"\" OR " +
        "(cql.allRecords=1 NOT vendorDetail.vendorAccount=\"\"))";
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

  private void persistPoLineIds(ChunkContext chunkContext, List<CompositePurchaseOrder> compOrders) throws JsonProcessingException {
    var polineIds = compOrders.stream()
      .flatMap(ord -> ord.getCompositePoLines().stream())
      .map(CompositePoLine::getId)
      .toList();
    ExecutionContextUtils.addToJobExecutionContext(chunkContext.getStepContext().getStepExecution(),"polineIds", ediObjectMapper.writeValueAsString(polineIds),"");
  }

  private String fieldInListFilter(String fieldName, List<?> list) {
    return String.format(" AND %s==%s", fieldName,
      list.stream()
      .map(item -> String.format("\"%s\"", item.toString()))
      .collect(Collectors.joining(" OR ", "(", ")")));
  }

  private List<CompositePurchaseOrder> assembleCompositeOrders(List<PurchaseOrder> orders, List<PoLine> poLines) {
    Map<String, List<CompositePoLine>> orderIdToCompositePoLines = poLines.stream()
      .map(poLine -> convertTo(poLine, CompositePoLine.class))
      .collect(groupingBy(CompositePoLine::getPurchaseOrderId));
    return orders.stream()
      .map(order -> convertTo(order, CompositePurchaseOrder.class))
      .map(compPo -> compPo.compositePoLines(
        requireNonNullElse(orderIdToCompositePoLines.get(compPo.getId().toString()), List.of())))
      .toList();
  }

  private <T> T convertTo(Object value, Class<T> c) {
    try {
      return ediObjectMapper.readValue(ediObjectMapper.writeValueAsString(value), c);
    } catch (JsonProcessingException ex) {
      throw new CompositeOrderMappingException(String.format("%s for object %s", ex.getMessage(), value));
    }
  }
}
