package org.folio.dew.batch.acquisitions.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.client.EmailClient;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.email.Attachment;
import org.folio.dew.domain.dto.email.EmailEntity;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static org.folio.dew.domain.dto.JobParameterNames.*;

@Log4j2
@StepScope
@Component
@RequiredArgsConstructor
public class SendToEmailTasklet implements Tasklet {

  private final ObjectMapper ediObjectMapper;
  private final EmailClient emailClient;
  private final FolioExecutionContext folioExecutionContext;

  @Override
  @SneakyThrows
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var exportConfig = ediObjectMapper.readValue((String) chunkContext.getStepContext().getJobParameters().get(EDIFACT_ORDERS_EXPORT), VendorEdiOrdersExportConfig.class);
    var jobId = (String) chunkContext.getStepContext().getJobParameters().get(JOB_ID);

    var emailEntity = new EmailEntity();
    emailEntity.setNotificationId(jobId);
    emailEntity.setFrom(exportConfig.getEdiEmail().getEmailFrom());
    emailEntity.setTo(exportConfig.getEdiEmail().getEmailTo());
    emailEntity.setHeader("Testsubject");
    emailEntity.setBody("Kleiner Testbody");

    var stepExecution = chunkContext.getStepContext().getStepExecution();
    var fileName = (String) ExecutionContextUtils.getExecutionVariable(stepExecution, ACQ_EXPORT_FILE_NAME);
    var edifactOrderAsString = (String) ExecutionContextUtils.getExecutionVariable(stepExecution, ACQ_EXPORT_FILE);

    var attachment = new Attachment();
    attachment.setName(fileName);
    attachment.setData(java.util.Base64.getEncoder().encodeToString(edifactOrderAsString.getBytes(StandardCharsets.UTF_8)));
    attachment.setContentType("application/EDIFACT");

    emailEntity.setAttachments(java.util.List.of(attachment));

    log.info("SendToEmailTasklet:: okapiUrl='{}', tenant='{}'",
      folioExecutionContext.getOkapiUrl(), folioExecutionContext.getTenantId());
    emailClient.sendEmail(emailEntity);

    return RepeatStatus.FINISHED;
  }

}
