package org.folio.dew.domain.dto.bursarfeesfines;

import java.io.Serializable;
import javax.annotation.CheckForNull;
import lombok.Builder;
import lombok.Data;
import lombok.With;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.User;

@Data
@With
@Builder
public class AccountWithAncillaryData implements Serializable {

  private Account account;

  @CheckForNull
  private Item item;

  private User user;
}
