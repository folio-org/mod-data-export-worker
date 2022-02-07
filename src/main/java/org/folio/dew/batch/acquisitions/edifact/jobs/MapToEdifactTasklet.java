package org.folio.dew.batch.acquisitions.edifact.jobs;

import java.util.List;
import java.util.stream.Collectors;

import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.edifact.PurchaseOrdersToEdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
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
  private final ObjectMapper objectMapper;

  private final OrdersService ordersService;
  private final PurchaseOrdersToEdifactMapper purchaseOrdersToEdifactMapper;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    log.info("Execute MapToEdifactTasklet");
    var stepExecution = chunkContext.getStepContext().getStepExecution();
    var jobParameters = chunkContext.getStepContext().getJobParameters();
    var ediExportConfig = objectMapper.readValue((String)jobParameters.get("edifactOrdersExport"), VendorEdiOrdersExportConfig.class);

    var compOrders = getCompPOList(ediExportConfig);
    // save poLineIds in memory
    persistPoLineIds(chunkContext, compOrders);

    var edifactOrderAsString = purchaseOrdersToEdifactMapper.convertOrdersToEdifact(compOrders);
    //save edifact file content in memory
    ExecutionContextUtils.addToJobExecutionContext(stepExecution, "edifactOrderAsString", edifactOrderAsString, "");
    return RepeatStatus.FINISHED;
  }

  private List<CompositePurchaseOrder> getCompPOList(VendorEdiOrdersExportConfig ediConfig) {
    var poQuery = "workflowStatus==Open";

    var orders = ordersService.getCompositePurchaseOrderByQuery(poQuery);

    var compOrders = orders.getPurchaseOrders()
      .stream()
      .map(order -> ordersService.getCompositePurchaseOrderById(order.getId()))
      .map(order -> order.compositePoLines(poLineFilteredOrder(order, ediConfig)))
      .filter(order -> !order.getCompositePoLines().isEmpty())
      .collect(Collectors.toList());

    log.info("comporders: {}", compOrders);
    return compOrders;
  }

  private void persistPoLineIds(ChunkContext chunkContext, List<CompositePurchaseOrder> compOrders) throws JsonProcessingException {
    var polineIds = compOrders.stream()
      .flatMap(ord -> ord.getCompositePoLines().stream())
      .map(CompositePoLine::getId)
      .collect(Collectors.toList());
    ExecutionContextUtils.addToJobExecutionContext(chunkContext.getStepContext().getStepExecution(),"polineIds", objectMapper.writeValueAsString(polineIds),"");
  }

  private List<CompositePoLine> poLineFilteredOrder(CompositePurchaseOrder order, VendorEdiOrdersExportConfig ediConfig) {
    return order.getCompositePoLines().stream()
          .collect(Collectors.toList());
  }

}
