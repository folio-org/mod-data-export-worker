package org.folio.dew.domain.dto.bursarfeesfines;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.folio.dew.domain.dto.Account;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

class AggregatedAccountsByUserTest {

  @Test
  void testFindTotalAmount() {
    AggregatedAccountsByUser aggregatedAccountsByUser = AggregatedAccountsByUser.builder()
      .accounts(null)
      .user(null)
      .build();

    assertThat(aggregatedAccountsByUser.findTotalAmount(), is(BigDecimal.ZERO));

    aggregatedAccountsByUser.setAccounts(new ArrayList<Account>());
    for (int i = 0; i < 10; i++) {
      Account account = new Account();
      account.setAmount(new BigDecimal(100));
      aggregatedAccountsByUser.getAccounts()
        .add(account);
    }

    assertThat(aggregatedAccountsByUser.findTotalAmount(), is(new BigDecimal(1000)));
  }
}
