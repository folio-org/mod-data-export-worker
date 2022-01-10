package org.folio.dew.batch.bulkedit.jobs.rollbackjob;

import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BulkEditUpdateUserRecordsForRollBackWriterTest {

  @Mock
  private UserClient userClient;
  @InjectMocks
  private BulkEditUpdateUserRecordsForRollBackWriter writer;

  @Test
  void  writeTest() throws Exception {
    var user1 = new User();
    user1.setId("1");
    var user2 = new User();
    user2.setId("2");
    var users = List.of(user1, user2);

    doNothing().when(userClient).updateUser(isA(User.class), isA(String.class));
    writer.write(users);
    verify(userClient, times(2)).updateUser(isA(User.class), isA(String.class));
  }
}
