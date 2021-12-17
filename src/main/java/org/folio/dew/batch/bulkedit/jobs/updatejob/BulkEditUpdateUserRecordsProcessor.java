package org.folio.dew.batch.bulkedit.jobs.updatejob;

import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.service.BulkEditParseService;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@Qualifier("updateUserRecordsProcessor")
@RequiredArgsConstructor
public class BulkEditUpdateUserRecordsProcessor implements ItemProcessor<UserFormat, User> {

  private final BulkEditParseService bulkEditParseService;

  @Override
  public User process(UserFormat userFormat) throws Exception {
    try {
      return bulkEditParseService.mapUserFormatToUser(userFormat);
    } catch (Exception e) {
      log.debug("Cannot process user record. Skipping a user.");
      return null;
    }
  }

}
