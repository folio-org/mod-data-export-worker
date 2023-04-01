package org.folio.dew.domain.dto.bursarfeesfines;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.User;

@Data
@Builder
public class AccountWithAncillaryData implements Serializable {

  private Account account;
  private Item item;
  private User user;
}
