package org.folio.dew.batch.acquisitions.jobs;

import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.groupingBy;
import static org.folio.dew.batch.acquisitions.jobs.EdifactExportJobConfig.POL_MEM_KEY;
import static org.folio.dew.batch.acquisitions.utils.ExportConfigFields.FILE_FORMAT;
import static org.folio.dew.batch.acquisitions.utils.ExportConfigFields.INTEGRATION_TYPE;
import static org.folio.dew.batch.acquisitions.utils.ExportConfigFields.TRANSMISSION_METHOD;
import static org.folio.dew.batch.acquisitions.utils.ExportUtils.generateFileName;
import static org.folio.dew.batch.acquisitions.utils.ExportUtils.validateField;
import static org.folio.dew.batch.acquisitions.utils.ExportUtils.validateFtpFields;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.exceptions.CompositeOrderMappingException;
import org.folio.dew.batch.acquisitions.exceptions.EdifactException;
import org.folio.dew.batch.acquisitions.mapper.ExportResourceMapper;
import org.folio.dew.batch.acquisitions.services.OrdersService;
import org.folio.dew.batch.acquisitions.services.OrganizationsService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.PurchaseOrder;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.acquisitions.edifact.ExportHolder;
import org.folio.dew.error.NotFoundException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.xlate.edi.stream.EDIStreamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public abstract class MapToEdifactTasklet implements Tasklet {

  private final ObjectMapper ediObjectMapper;
  private final OrganizationsService organizationsService;
  protected final OrdersService ordersService;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    log.info("execute:: Executing MapToEdifactTasklet with job: {}", chunkContext.getStepContext().getJobName());
    var jobParameters = chunkContext.getStepContext().getJobParameters();
    var ediExportConfig = ediObjectMapper.readValue((String) jobParameters.get(EDIFACT_ORDERS_EXPORT), VendorEdiOrdersExportConfig.class);
    validateEdiExportConfig(ediExportConfig);

    var holder = buildEdifactExportHolder(ediExportConfig, jobParameters);
    persistPoLineIds(chunkContext, holder.orders());

    String jobName = jobParameters.get(JobParameterNames.JOB_NAME).toString();
    var edifactStringResult = getExportResourceMapper(ediExportConfig).convertForExport(holder.orders(), holder.pieces(), ediExportConfig, jobName);

    // save edifact file content and name in memory
    var stepExecution = chunkContext.getStepContext().getStepExecution();
    ExecutionContextUtils.addToJobExecutionContext(stepExecution, ACQ_EXPORT_FILE, edifactStringResult, "");
    ExecutionContextUtils.addToJobExecutionContext(stepExecution, ACQ_EXPORT_FILE_NAME, getFileName(ediExportConfig), "");
    return RepeatStatus.FINISHED;
  }

  private void validateEdiExportConfig(VendorEdiOrdersExportConfig ediExportConfig) {
    var missingFields = getExportConfigMissingFields(ediExportConfig);
    validateField(INTEGRATION_TYPE.getName(), ediExportConfig.getIntegrationType(), Objects::nonNull, missingFields);
    validateField(TRANSMISSION_METHOD.getName(), ediExportConfig.getTransmissionMethod(), Objects::nonNull, missingFields);
    validateField(FILE_FORMAT.getName(), ediExportConfig.getFileFormat(), Objects::nonNull, missingFields);
    validateFtpFields(ediExportConfig, missingFields);

    if (!missingFields.isEmpty()) {
      throw new EdifactException("Export configuration is incomplete, missing required fields: %s".formatted(missingFields));
    }
  }

  protected List<CompositePurchaseOrder> getCompositeOrders(String poLineQuery) {
    var poLines = ordersService.getPoLinesByQuery(poLineQuery);
    var orderIds = poLines.stream()
      .map(PoLine::getPurchaseOrderId)
      .distinct()
      .toList();
    var compOrders = assembleCompositeOrders(ordersService.getPurchaseOrdersByIds(orderIds), poLines);

    log.debug("getCompositeOrders:: {}", compOrders);
    if (compOrders.isEmpty()) {
      throw new NotFoundException(PurchaseOrder.class);
    }
    return compOrders;
  }

  protected void persistPoLineIds(ChunkContext chunkContext, List<CompositePurchaseOrder> compOrders) throws JsonProcessingException {
    var poLineIds = compOrders.stream()
      .flatMap(ord -> ord.getPoLines().stream())
      .map(PoLine::getId)
      .toList();
    ExecutionContextUtils.addToJobExecutionContext(chunkContext.getStepContext().getStepExecution(), POL_MEM_KEY, ediObjectMapper.writeValueAsString(poLineIds), "");
  }

  private List<CompositePurchaseOrder> assembleCompositeOrders(List<PurchaseOrder> orders, List<PoLine> poLines) {
    Map<String, List<PoLine>> orderIdToPoLines = poLines.stream()
      .collect(groupingBy(PoLine::getPurchaseOrderId));
    return orders.stream()
      .map(order -> convertTo(order, CompositePurchaseOrder.class))
      .map(compPo -> compPo.poLines(
        requireNonNullElse(orderIdToPoLines.get(compPo.getId().toString()), List.of())))
      .toList();
  }

  private String getFileName(VendorEdiOrdersExportConfig ediExportConfig) {
    var vendorName = organizationsService.getOrganizationById(ediExportConfig.getVendorId().toString()).get("code").asText();
    var configName = ediExportConfig.getConfigName();
    var fileFormat = ediExportConfig.getFileFormat();
    return generateFileName(vendorName, configName, fileFormat);
  }

  private <T> T convertTo(Object value, Class<T> c) {
    try {
      return ediObjectMapper.readValue(ediObjectMapper.writeValueAsString(value), c);
    } catch (JsonProcessingException ex) {
      throw new CompositeOrderMappingException(String.format("%s for object %s", ex.getMessage(), value));
    }
  }

  protected abstract ExportResourceMapper getExportResourceMapper(VendorEdiOrdersExportConfig ediOrdersExportConfig);

  protected abstract List<String> getExportConfigMissingFields(VendorEdiOrdersExportConfig ediOrdersExportConfig);

  protected abstract ExportHolder buildEdifactExportHolder(VendorEdiOrdersExportConfig ediExportConfig, Map<String, Object> jobParameters)
    throws JsonProcessingException, EDIStreamException;

}
