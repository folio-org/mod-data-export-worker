package org.folio.dew.domain.dto.bursarfeesfines;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.User;

@Data
@Builder
public class AggregatedAccountsByUser {

  private User user;
  private List<Account> accounts;
}
