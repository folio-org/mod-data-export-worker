package org.folio.dew.domain.dto.bursarfeefines;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.ArrayList;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
import org.junit.jupiter.api.Test;

public class AggregatedAccountsByUserTest {

  @Test
  void testFindTotalAmount() {
    AggregatedAccountsByUser aggregatedAccountsByUser = AggregatedAccountsByUser
      .builder()
      .accounts(null)
      .user(null)
      .build();

    assertThat(aggregatedAccountsByUser.findTotalAmount(), is(BigDecimal.ZERO));

    aggregatedAccountsByUser.setAccounts(new ArrayList<Account>());
    for (int i = 0; i < 10; i++) {
      Account account = new Account();
      account.setAmount(new BigDecimal(100));
      aggregatedAccountsByUser.getAccounts().add(account);
    }

    assertThat(
      aggregatedAccountsByUser.findTotalAmount(),
      is(new BigDecimal(1000))
    );
  }
}
