package org.folio.dew.batch.bulkedit.jobs.updatejob;

import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.NO_CHANGE_MESSAGE;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditChangedRecordsService;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.BulkEditRollBackService;
import org.folio.dew.service.BulkEditStatisticService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@Qualifier("updateUserRecordsWriter")
@RequiredArgsConstructor
@JobScope
public class BulkEditUpdateUserRecordsWriter implements ItemWriter<User> {

  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobExecution}")
  private JobExecution jobExecution;

  private final UserClient userClient;
  private final BulkEditRollBackService bulkEditRollBackService;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  private final BulkEditStatisticService bulkEditStatisticService;
  private final BulkEditChangedRecordsService changedRecordsService;

  @Override
  public void write(Chunk<? extends User> users) throws Exception {
    users.forEach(user -> {
      try {
        var initialUser = userClient.getUserById(user.getId());
        initialUser.setMetadata(null);
        if (initialUser.equals(user)) {
          log.info("User with barcode={}: {}", user.getBarcode(), NO_CHANGE_MESSAGE);
          bulkEditProcessingErrorsService.saveErrorInCSV(jobId, initialUser.getBarcode(), new BulkEditException(NO_CHANGE_MESSAGE), FilenameUtils.getName(jobExecution.getJobParameters().getString(FILE_NAME)));
          changedRecordsService.removeUserId(user.getId(), jobId);
        } else {
          userClient.updateUser(user, user.getId());
          log.info("Update user with barcode={} by job id {}", user.getBarcode(), jobId);
          bulkEditStatisticService.incrementSuccess();
          bulkEditRollBackService.putUserIdForJob(user.getId(), UUID.fromString(jobId));
        }
      } catch (Exception e) {
        log.error("Cannot update user with barcode={}. Reason: {}",  user.getBarcode(), e.getMessage());
        bulkEditProcessingErrorsService.saveErrorInCSV(jobId, user.getBarcode(), new BulkEditException(e.getMessage()), FilenameUtils.getName(jobExecution.getJobParameters().getString(FILE_NAME)));
        changedRecordsService.removeUserId(user.getId(), jobId);
      }
    });
  }
}
