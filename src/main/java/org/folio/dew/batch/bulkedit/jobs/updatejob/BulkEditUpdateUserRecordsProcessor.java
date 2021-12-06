package org.folio.dew.batch.bulkedit.jobs.updatejob;

import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.service.BulkEditParseService;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BulkEditUpdateUserRecordsProcessor implements ItemProcessor<UserFormat, User> {

  private BulkEditParseService bulkEditParseService;

  @Override
  public User process(UserFormat userFormat) throws Exception {
    return bulkEditParseService.mapUserFormatToUser(userFormat);
  }

}
