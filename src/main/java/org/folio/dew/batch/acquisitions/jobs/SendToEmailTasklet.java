package org.folio.dew.batch.acquisitions.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.client.EmailClient;
import org.folio.dew.client.TemplateEngineClient;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.email.Attachment;
import org.folio.dew.domain.dto.email.EmailEntity;
import org.folio.dew.domain.dto.templateengine.TemplateProcessingRequest;
import org.folio.spring.FolioExecutionContext;
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

import static org.folio.dew.domain.dto.JobParameterNames.*;

@Log4j2
@StepScope
@Component
@RequiredArgsConstructor
public class SendToEmailTasklet implements Tasklet {

  private final ObjectMapper ediObjectMapper;
  private final EmailClient emailClient;
  private final TemplateEngineClient templateEngineClient;
  private final FolioExecutionContext folioExecutionContext;

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

    var attachment = new Attachment();
    attachment.setName(fileName);
    attachment.setData(Base64.getEncoder().encodeToString(edifactOrderAsString.getBytes(StandardCharsets.UTF_8)));
    attachment.setContentType("application/EDIFACT");
    emailEntity.setAttachments(List.of(attachment));

    log.info("SendToEmailTasklet:: okapiUrl='{}', tenant='{}'",
      folioExecutionContext.getOkapiUrl(), folioExecutionContext.getTenantId());
    emailClient.sendEmail(emailEntity);

    return RepeatStatus.FINISHED;
  }

  private String[] resolveTemplate(VendorEdiOrdersExportConfig exportConfig, String fileName) {
    var templateId = Optional.ofNullable(exportConfig.getEdiEmail())
      .map(e -> e.getEmailTemplate())
      .orElse(null);

    if (templateId == null) {
      log.warn("SendToEmailTasklet:: no emailTemplate configured, using empty subject and body");
      return new String[]{"", ""};
    }

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

    log.info("SendToEmailTasklet:: calling template-engine with templateId='{}', configName='{}', fileName='{}', pieceCount={}",
      templateId, exportConfig.getConfigName(), fileName, pieceCount);
    JsonNode response = templateEngineClient.processTemplate(request);
    JsonNode result = response.path("result");
    log.info("SendToEmailTasklet:: template-engine response: header='{}', body='{}'",
      result.path("header").asText(""), result.path("body").asText(""));
    return new String[]{result.path("header").asText(""), result.path("body").asText("")};
  }

}
