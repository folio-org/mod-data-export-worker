package org.folio.dew.batch.bursarfeesfines.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportFilter;
import org.folio.dew.domain.dto.BursarExportFilterAge;
import org.folio.dew.domain.dto.BursarExportFilterAmount;
import org.folio.dew.domain.dto.BursarExportFilterCondition;
import org.folio.dew.domain.dto.BursarExportFilterFeeFineOwner;
import org.folio.dew.domain.dto.BursarExportFilterFeeType;
import org.folio.dew.domain.dto.BursarExportFilterLocation;
import org.folio.dew.domain.dto.BursarExportFilterNegation;
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.BursarExportFilterPatronGroup;
import org.folio.dew.domain.dto.BursarExportFilterServicePoint;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemLocation;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.folio.dew.helpers.bursarfeesfines.InvalidBursarExportFilter;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

class BursarFilterEvaluatorTest {

  @Test
  void testJsonNullableExportFilter() {
    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(null)
      .user(null)
      .item(null)
      .build();

    BursarExportFilterPass bursarExportFilterPass = new BursarExportFilterPass();

    JsonNullable<BursarExportFilter> jsonNullableFilterPass = JsonNullable.of(
      bursarExportFilterPass
    );

    assertThat(
      BursarFilterEvaluator.evaluate(
        accountWithAncillaryData,
        jsonNullableFilterPass
      ),
      is(true)
    );

    assertThat(
      BursarFilterEvaluator.evaluate(
        accountWithAncillaryData,
        JsonNullable.<BursarExportFilter>undefined()
      ),
      is(true)
    );
  }

  @Test
  void testFilterAccountByAge() {
    Account account = new Account();

    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(account)
      .user(null)
      .item(null)
      .build();

    BursarExportFilterAge filterAge = new BursarExportFilterAge();
    filterAge.setNumDays(5);
    filterAge.setCondition(BursarExportFilterAge.ConditionEnum.GREATER_THAN);

    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge),
      is(true)
    );

    account.setDateCreated(new Date());
    accountWithAncillaryData.setAccount(account);

    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge),
      is(false)
    );

    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, -10);
    account.setDateCreated(calendar.getTime());
    accountWithAncillaryData.setAccount(account);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge),
      is(true)
    );
    calendar.add(Calendar.DAY_OF_MONTH, 8);
    account.setDateCreated(calendar.getTime());
    accountWithAncillaryData.setAccount(account);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge),
      is(false)
    );

    filterAge.setCondition(
      BursarExportFilterAge.ConditionEnum.GREATER_THAN_EQUAL
    );
    calendar.add(Calendar.DAY_OF_MONTH, -3);
    account.setDateCreated(calendar.getTime());
    accountWithAncillaryData.setAccount(account);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge),
      is(true)
    );
    calendar.add(Calendar.DAY_OF_MONTH, 2);
    account.setDateCreated(calendar.getTime());
    accountWithAncillaryData.setAccount(account);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge),
      is(false)
    );

    calendar.add(Calendar.DAY_OF_MONTH, -2);
    account.setDateCreated(calendar.getTime());
    accountWithAncillaryData.setAccount(account);
    filterAge.setCondition(BursarExportFilterAge.ConditionEnum.LESS_THAN_EQUAL);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge),
      is(true)
    );
    calendar.add(Calendar.DAY_OF_MONTH, -5);
    account.setDateCreated(calendar.getTime());
    accountWithAncillaryData.setAccount(account);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge),
      is(false)
    );

    calendar.add(Calendar.DAY_OF_MONTH, 8);
    account.setDateCreated(calendar.getTime());
    accountWithAncillaryData.setAccount(account);
    filterAge.setCondition(BursarExportFilterAge.ConditionEnum.LESS_THAN);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge),
      is(true)
    );
    calendar.add(Calendar.DAY_OF_MONTH, -5);
    account.setDateCreated(calendar.getTime());
    accountWithAncillaryData.setAccount(account);
    filterAge.setCondition(BursarExportFilterAge.ConditionEnum.LESS_THAN);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge),
      is(false)
    );
  }

  @Test
  void testFilterAccountByAmount() {
    BursarExportFilterAmount filterAmount = new BursarExportFilterAmount();

    Account account = new Account();
    account.setAmount(new BigDecimal(50));
    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(account)
      .user(null)
      .item(null)
      .build();

    // test for accounts less than filter value
    filterAmount.setAmount(6000);
    filterAmount.setCondition(BursarExportFilterAmount.ConditionEnum.LESS_THAN);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAmount),
      is(true)
    );
    filterAmount.setAmount(5000);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAmount),
      is(false)
    );

    // test for accounts less than or equal to filter value
    filterAmount.setCondition(
      BursarExportFilterAmount.ConditionEnum.LESS_THAN_EQUAL
    );
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAmount),
      is(true)
    );
    filterAmount.setAmount(4000);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAmount),
      is(false)
    );

    // test for accounts greater than filter value
    filterAmount.setCondition(
      BursarExportFilterAmount.ConditionEnum.GREATER_THAN
    );
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAmount),
      is(true)
    );
    filterAmount.setAmount(5000);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAmount),
      is(false)
    );

    // test for accounts less than filter value
    filterAmount.setCondition(
      BursarExportFilterAmount.ConditionEnum.GREATER_THAN_EQUAL
    );
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAmount),
      is(true)
    );
    filterAmount.setAmount(6000);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAmount),
      is(false)
    );
  }

  @Test
  void testFilterAccountByFeeType() {
    UUID feeFineTypeUUID = UUID.fromString(
      "00000000-0000-4000-8000-000000000000"
    );
    BursarExportFilterFeeType filterFeeType = new BursarExportFilterFeeType();
    filterFeeType.setFeeFineTypeId(feeFineTypeUUID);

    Account account = new Account();
    account.setFeeFineId("00000000-0000-4000-8000-000000000000");

    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(account)
      .user(null)
      .item(null)
      .build();

    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterFeeType),
      is(true)
    );
  }

  @Test
  void testFilterAccountByFeeFineOwner() {
    UUID feeFineOwnerUUID = UUID.fromString(
      "00000000-0000-4000-8000-000000000000"
    );
    BursarExportFilterFeeFineOwner filterFeeFineOwner = new BursarExportFilterFeeFineOwner();
    filterFeeFineOwner.setFeeFineOwner(feeFineOwnerUUID);

    Account account = new Account();
    account.setFeeFineOwner("00000000-0000-4000-8000-000000000000");

    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(account)
      .user(null)
      .item(null)
      .build();

    assertThat(
      BursarFilterEvaluator.evaluate(
        accountWithAncillaryData,
        filterFeeFineOwner
      ),
      is(true)
    );
  }

  @Test
  void testFilterAccountByItemLocation() {
    UUID itemLocationID = UUID.fromString(
      "00000000-0000-4000-8000-000000000000"
    );
    BursarExportFilterLocation filterLocation = new BursarExportFilterLocation();
    filterLocation.setLocationId(itemLocationID);

    Item item = new Item();
    ItemLocation itemLocation = new ItemLocation();
    itemLocation.setId("00000000-0000-4000-8000-000000000000");
    item.setEffectiveLocation(itemLocation);

    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(null)
      .user(null)
      .item(item)
      .build();

    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterLocation),
      is(true)
    );
  }

  @Test
  void testFilterAccountByPatronGroup() {
    UUID patronGroupID = UUID.fromString(
      "00000000-0000-4000-8000-000000000000"
    );
    BursarExportFilterPatronGroup filterPatronGroup = new BursarExportFilterPatronGroup();
    filterPatronGroup.setPatronGroupId(patronGroupID);

    User user = new User();
    user.setPatronGroup("00000000-0000-4000-8000-000000000000");

    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(null)
      .user(user)
      .item(null)
      .build();

    assertThat(
      BursarFilterEvaluator.evaluate(
        accountWithAncillaryData,
        filterPatronGroup
      ),
      is(true)
    );
  }

  @Test
  void testFilterAccountByServicePoint() {
    UUID servicePointID = UUID.fromString(
      "00000000-0000-4000-8000-000000000000"
    );
    BursarExportFilterServicePoint filterServicePoint = new BursarExportFilterServicePoint();
    filterServicePoint.setServicePointId(servicePointID);

    Item item = new Item();
    item.setInTransitDestinationServicePointId(
      "00000000-0000-4000-8000-000000000000"
    );

    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(null)
      .user(null)
      .item(item)
      .build();

    assertThat(
      BursarFilterEvaluator.evaluate(
        accountWithAncillaryData,
        filterServicePoint
      ),
      is(true)
    );
  }

  @Test
  void testNegationFilter() {
    BursarExportFilterNegation filterNegation = new BursarExportFilterNegation();
    filterNegation.setCriteria(new BursarExportFilterPass());

    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(null)
      .user(null)
      .item(null)
      .build();

    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterNegation),
      is(false)
    );

    BursarExportFilterAmount filterAmount = new BursarExportFilterAmount();
    filterAmount.setCondition(
      BursarExportFilterAmount.ConditionEnum.GREATER_THAN
    );
    filterAmount.setAmount(6000);

    Account account = new Account();
    account.setAmount(new BigDecimal(50));

    accountWithAncillaryData.setAccount(account);

    filterNegation.setCriteria(filterAmount);

    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterNegation),
      is(true)
    );
  }

  @Test
  void testConditionalFilters() {
    BursarExportFilterCondition filterCondition = new BursarExportFilterCondition();

    BursarExportFilterPass filterPass = new BursarExportFilterPass();

    BursarExportFilterNegation notFilterPass = new BursarExportFilterNegation();
    notFilterPass.setCriteria(filterPass);

    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(null)
      .user(null)
      .item(null)
      .build();

    filterCondition.addCriteriaItem(filterPass);
    filterCondition.addCriteriaItem(notFilterPass);

    // testing AND filter
    filterCondition.setOperation(BursarExportFilterCondition.OperationEnum.AND);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterCondition),
      is(false)
    );

    // testing OR filter
    filterCondition.setOperation(BursarExportFilterCondition.OperationEnum.OR);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterCondition),
      is(true)
    );
  }

  @Test
  void testInvalidFilter() {
    InvalidBursarExportFilter invalidBursarExportFilter = new InvalidBursarExportFilter();

    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(null)
      .user(null)
      .item(null)
      .build();

    assertThat(
      BursarFilterEvaluator.evaluate(
        accountWithAncillaryData,
        invalidBursarExportFilter
      ),
      is(true)
    );
  }
}
