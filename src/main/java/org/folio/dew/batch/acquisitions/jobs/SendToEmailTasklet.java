package org.folio.dew.batch.acquisitions.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.exceptions.EdifactException;
import org.folio.dew.client.EmailClient;
import org.folio.dew.client.TemplateEngineClient;
import org.folio.dew.domain.dto.EdiEmail;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.email.Attachment;
import org.folio.dew.domain.dto.email.EmailEntity;
import org.folio.dew.domain.dto.templateengine.TemplateProcessingRequest;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.folio.dew.domain.dto.JobParameterNames.*;

@Log4j2
@StepScope
@Component
@RequiredArgsConstructor
public class SendToEmailTasklet implements Tasklet {

  private static final String EMAIL_ERROR_MESSAGE = "Failed to send the email";
  private static final String TEMPLATE_ENGINE_ERROR_MESSAGE = "Failed to retrieve the template";

  private final ObjectMapper ediObjectMapper;
  private final EmailClient emailClient;
  private final TemplateEngineClient templateEngineClient;

  @Override
  @SneakyThrows
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var exportConfig = ediObjectMapper.readValue(
      (String) chunkContext.getStepContext().getJobParameters().get(EDIFACT_ORDERS_EXPORT),
      VendorEdiOrdersExportConfig.class);
    var jobId = (String) chunkContext.getStepContext().getJobParameters().get(JOB_ID);

    var stepExecution = chunkContext.getStepContext().getStepExecution();
    var fileName = (String) ExecutionContextUtils.getExecutionVariable(stepExecution, ACQ_EXPORT_FILE_NAME);
    var edifactOrderAsString = (String) ExecutionContextUtils.getExecutionVariable(stepExecution, ACQ_EXPORT_FILE);

    var emailEntity = new EmailEntity();
    emailEntity.setNotificationId(jobId);
    emailEntity.setFrom(exportConfig.getEdiEmail().getEmailFrom());
    emailEntity.setTo(exportConfig.getEdiEmail().getEmailTo());

    var templateResult = resolveTemplate(exportConfig, fileName);
    emailEntity.setHeader(templateResult[0]);
    emailEntity.setBody(templateResult[1]);
    emailEntity.setOutputFormat(templateResult[2]);

    emailEntity.setAttachments(List.of(buildAttachment(fileName, edifactOrderAsString, exportConfig.getFileFormat())));

    sendEmail(emailEntity);
    return RepeatStatus.FINISHED;
  }

  private String[] resolveTemplate(VendorEdiOrdersExportConfig exportConfig, String fileName) {
    UUID templateId = Optional.ofNullable(exportConfig.getEdiEmail())
      .map(EdiEmail::getEmailTemplate)
      .orElse(null);

    int pieceCount = Optional.ofNullable(exportConfig.getClaimPieceIds())
      .map(List::size)
      .orElse(0);

    var request = TemplateProcessingRequest.builder()
      .templateId(templateId)
      .context(TemplateProcessingRequest.ClaimsContext.builder()
        .configName(exportConfig.getConfigName())
        .fileName(fileName)
        .exportDate(LocalDate.now().toString())
        .pieceCount(pieceCount)
        .build())
      .build();

    try {
      JsonNode response = templateEngineClient.processTemplate(request);
      JsonNode result = response.path("result");
      String outputFormat = response.path("meta").path("outputFormat").asText(request.getOutputFormat());
      return new String[]{result.path("header").asText(), result.path("body").asText(), outputFormat};
    } catch (Exception e) {
      log.error(TEMPLATE_ENGINE_ERROR_MESSAGE, e);
      throw new EdifactException(TEMPLATE_ENGINE_ERROR_MESSAGE);
    }
  }

  private Attachment buildAttachment(String fileName, String fileContent, VendorEdiOrdersExportConfig.FileFormatEnum fileFormat) {
    var contentType = fileFormat == VendorEdiOrdersExportConfig.FileFormatEnum.CSV
      ? "text/csv"
      : "application/EDIFACT";
    var attachment = new Attachment();
    attachment.setName(fileName);
    attachment.setData(Base64.getEncoder().encodeToString(fileContent.getBytes(StandardCharsets.UTF_8)));
    attachment.setContentType(contentType);
    return attachment;
  }

  private void sendEmail(EmailEntity emailEntity) {
    try {
      emailClient.sendEmail(emailEntity);
    } catch (Exception e) {
      log.error(EMAIL_ERROR_MESSAGE, e);
      throw new EdifactException(EMAIL_ERROR_MESSAGE);
    }
  }

}
