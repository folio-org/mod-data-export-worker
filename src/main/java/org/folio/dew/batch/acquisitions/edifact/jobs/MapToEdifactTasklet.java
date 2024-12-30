package org.folio.dew.batch.acquisitions.edifact.jobs;

import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.groupingBy;
import static org.folio.dew.batch.acquisitions.edifact.jobs.EdifactExportJobConfig.POL_MEM_KEY;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.FILE_FORMAT;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.FTP_PORT;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.INTEGRATION_TYPE;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.SERVER_ADDRESS;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.TRANSMISSION_METHOD;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportUtils.generateFileName;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportUtils.validateField;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.edifact.exceptions.CompositeOrderMappingException;
import org.folio.dew.batch.acquisitions.edifact.exceptions.EdifactException;
import org.folio.dew.batch.acquisitions.edifact.mapper.ExportResourceMapper;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.batch.acquisitions.edifact.services.OrganizationsService;
import org.folio.dew.domain.dto.CompositePoLine;
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
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

@SuperBuilder
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

    if (ediExportConfig.getTransmissionMethod() == FTP) {
      var ftpConfig = ediExportConfig.getEdiFtp();
      validateField(FTP_PORT.getName(), ftpConfig.getFtpPort(), Objects::nonNull, missingFields);
      validateField(SERVER_ADDRESS.getName(), ftpConfig.getServerAddress(), StringUtils::isNotEmpty, missingFields);
    }

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
      .flatMap(ord -> ord.getCompositePoLines().stream())
      .map(CompositePoLine::getId)
      .toList();
    ExecutionContextUtils.addToJobExecutionContext(chunkContext.getStepContext().getStepExecution(), POL_MEM_KEY, ediObjectMapper.writeValueAsString(poLineIds), "");
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
