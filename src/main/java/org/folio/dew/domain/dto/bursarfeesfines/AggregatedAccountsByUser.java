package org.folio.dew.domain.dto.bursarfeesfines;

import java.math.BigDecimal;
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

  public BigDecimal findTotalAmount() {
    BigDecimal totalAmount = new BigDecimal(0);
    for (Account account : accounts) {
      totalAmount = totalAmount.add(account.getAmount());
    }

    return totalAmount;
  }
}
