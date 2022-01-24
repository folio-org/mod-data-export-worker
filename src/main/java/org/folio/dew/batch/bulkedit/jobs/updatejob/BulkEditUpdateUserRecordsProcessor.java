package org.folio.dew.batch.bulkedit.jobs.updatejob;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditParseService;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static org.folio.dew.utils.Constants.FILE_NAME;

@Log4j2
@Component
@Qualifier("updateUserRecordsProcessor")
@RequiredArgsConstructor
@JobScope
public class BulkEditUpdateUserRecordsProcessor implements ItemProcessor<UserFormat, User> {

  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobExecution}")
  private JobExecution jobExecution;

  private final BulkEditParseService bulkEditParseService;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;

  @Override
  public User process(UserFormat userFormat) throws Exception {
    try {
      return bulkEditParseService.mapUserFormatToUser(userFormat);
    } catch (Exception e) {
      log.info("Error process user format {} : {}",  userFormat.getId(), e.getMessage());
      bulkEditProcessingErrorsService.saveErrorInCSV(jobId, userFormat.getBarcode(), new BulkEditException(e.getMessage()), FilenameUtils.getName(jobExecution.getJobParameters().getString(FILE_NAME)));
      return null;
    }
  }

}
