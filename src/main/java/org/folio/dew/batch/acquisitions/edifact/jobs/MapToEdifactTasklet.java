package org.folio.dew.batch.acquisitions.edifact.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.edifact.PurchaseOrdersToEdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.PoLineCollection;
import org.folio.dew.domain.dto.PurchaseOrderCollection;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.folio.dew.repository.SFTPObjectStorageRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Component
@StepScope
@Log4j2
public class MapToEdifactTasklet implements Tasklet {
  private final ObjectMapper objectMapper;

  private final OrdersService ordersService;
  private final PurchaseOrdersToEdifactMapper purchaseOrdersToEdifactMapper;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    log.info("Execute MapToEdifactTasklet");
    var jobParameters = chunkContext.getStepContext().getJobParameters();
    var ediExportConfig = objectMapper.readValue((String)jobParameters.get("edifactOrdersExport"), VendorEdiOrdersExportConfig.class);

    var poList = getCompPOList(ediExportConfig);
    CompositePurchaseOrder compo = ordersService.getCompositePurchaseOrderById("cf4bfd64-e609-4ecb-b3e6-7a5a1ced1645");

    var orderList = new ArrayList<CompositePurchaseOrder>();
    orderList.add(compo);

    var edifactOrderAsString = purchaseOrdersToEdifactMapper.convertOrdersToEdifact(orderList);

   ExecutionContextUtils
     .addToJobExecutionContext(chunkContext.getStepContext().getStepExecution(), "edifactOrderAsString", edifactOrderAsString, "");
    return RepeatStatus.FINISHED;
  }

  private PurchaseOrderCollection getCompositePurchaseOrderByQuery(String poQuery) {
    var polineQuery = "purchaseOrder.workflowStatus==Open";

    return ordersService.getCompositePurchaseOrderByQuery(poQuery);
  }

  private PoLineCollection getPoLineCollection(String polineQuery) {
    var poQuery = "workflowStatus==Open";

    return  ordersService.getPoLineByQuery(polineQuery);
  }

  private List<CompositePurchaseOrder> getCompPOList(VendorEdiOrdersExportConfig ediConfig) {
    var poQuery = "workflowStatus==Open"
      + String.format("&vendor==%s", ediConfig.getVendorId());

    var orders = ordersService.getCompositePurchaseOrderByQuery(poQuery);

    var compOrders = orders.getPurchaseOrders()
      .stream()
      .map(order -> ordersService.getCompositePurchaseOrderById(order.getId()))
      .map(order -> order.compositePoLines(poLineFilteredOrder(order, ediConfig)))
      .filter(order-> !order.getCompositePoLines().isEmpty())
      .collect(Collectors.toList());

    log.info("comporders: {}", compOrders);
    return compOrders;
  }

  private List<CompositePoLine> poLineFilteredOrder(CompositePurchaseOrder order, VendorEdiOrdersExportConfig ediConfig) {
    return order.getCompositePoLines().stream()
      .filter(CompositePoLine::getAutomaticExport)
      .collect(Collectors.toList());
  }

}
