package org.folio.dew.batch.bulkedit.jobs.rollbackjob;

import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.service.BulkEditParseService;
import org.folio.dew.service.BulkEditRollBackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkEditFilterUserRecordsForRollBackProcessorTest {

  @Mock
  private BulkEditRollBackService bulkEditRollBackService;
  @Mock
  private BulkEditParseService bulkEditParseService;

  @InjectMocks
  private BulkEditFilterUserRecordsForRollBackProcessor processor;

  @Test
  void testWriteIfUserBeRollBack() {
    var userFormat = new UserFormat();
    userFormat.setId("userId");
    var user= new User();
    user.setId("userId");

    when(bulkEditRollBackService.isUserBeRollBack(isA(String.class), isA(UUID.class))).thenReturn(true);
    when(bulkEditParseService.mapUserFormatToUser(isA(UserFormat.class))).thenReturn(user);

    processor.setJobId("edd30136-9a7b-4226-9e82-83024dbeac4a");
    var processedUser = processor.process(userFormat);
    verify(bulkEditParseService, times(1)).mapUserFormatToUser(isA(UserFormat.class));
    assertNotNull(processedUser);
  }

  @Test
  void testWriteIfUserNotBeRollBack() {
    var userFormat = new UserFormat();
    userFormat.setId("userId");

    when(bulkEditRollBackService.isUserBeRollBack(isA(String.class), isA(UUID.class))).thenReturn(false);

    processor.setJobId("edd30136-9a7b-4226-9e82-83024dbeac4a");
    var user = processor.process(userFormat);
    verify(bulkEditParseService, times(0)).mapUserFormatToUser(isA(UserFormat.class));
    assertNull(user);
  }
}
