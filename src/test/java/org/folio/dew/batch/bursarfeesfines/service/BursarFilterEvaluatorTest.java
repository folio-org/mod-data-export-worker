package org.folio.dew.batch.bursarfeesfines.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
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
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

@Log4j2
public class BursarFilterEvaluatorTest {

  @Test
  void evaluateJSONNullableExportFilter() {
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
  void filterAccountByAge() {
    Account account = new Account();
    account.setDateCreated(new Date());

    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(account)
      .user(null)
      .item(null)
      .build();

    BursarExportFilterAge filterAge = new BursarExportFilterAge();
    filterAge.setNumDays(1);

    assertFalse(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge)
    );

    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, -2);

    account.setDateCreated(calendar.getTime());
    accountWithAncillaryData.setAccount(account);

    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAge),
      is(true)
    );
  }

  @Test
  void filterAccountByAmount() {
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

    // test for accounts less than or equal to filter value
    filterAmount.setAmount(5000);
    filterAmount.setCondition(
      BursarExportFilterAmount.ConditionEnum.LESS_THAN_EQUAL
    );
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAmount),
      is(true)
    );

    // test for accounts greater than filter value
    filterAmount.setAmount(4000);
    filterAmount.setCondition(
      BursarExportFilterAmount.ConditionEnum.GREATER_THAN
    );
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAmount),
      is(true)
    );

    // test for accounts less than filter value
    filterAmount.setAmount(5000);
    filterAmount.setCondition(
      BursarExportFilterAmount.ConditionEnum.GREATER_THAN_EQUAL
    );
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterAmount),
      is(true)
    );
  }

  @Test
  void filterAccountByFeeType() {
    UUID feeFineTypeUUID = UUID.randomUUID();
    BursarExportFilterFeeType filterFeeType = new BursarExportFilterFeeType();
    filterFeeType.setFeeFineTypeId(feeFineTypeUUID);

    Account account = new Account();
    account.setFeeFineId(feeFineTypeUUID.toString());

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
  void filterAccountByFeeFineOwner() {
    UUID feeFineOwnerUUID = UUID.randomUUID();
    BursarExportFilterFeeFineOwner filterFeeFineOwner = new BursarExportFilterFeeFineOwner();
    filterFeeFineOwner.setFeeFineOwner(feeFineOwnerUUID);

    Account account = new Account();
    account.setFeeFineOwner(feeFineOwnerUUID.toString());

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
  void filterAccountByItemLocation() {
    UUID itemLocationID = UUID.randomUUID();
    BursarExportFilterLocation filterLocation = new BursarExportFilterLocation();
    filterLocation.setLocationId(itemLocationID);

    Item item = new Item();
    ItemLocation itemLocation = new ItemLocation();
    itemLocation.setId(itemLocationID.toString());
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
  void filterAccountByPatronGroup() {
    UUID patronGroupID = UUID.randomUUID();
    BursarExportFilterPatronGroup filterPatronGroup = new BursarExportFilterPatronGroup();
    filterPatronGroup.setPatronGroupId(patronGroupID);

    User user = new User();
    user.setPatronGroup(patronGroupID.toString());

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
  void filterAccountByServicePoint() {
    UUID servicePointID = UUID.randomUUID();
    BursarExportFilterServicePoint filterServicePoint = new BursarExportFilterServicePoint();
    filterServicePoint.setServicePointId(servicePointID);

    Item item = new Item();
    item.setInTransitDestinationServicePointId(servicePointID.toString());

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
  void evaluateNegationFilter() {
    BursarExportFilterNegation filterNegation = new BursarExportFilterNegation();
    filterNegation.setCriteria(new BursarExportFilterPass());

    AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .account(null)
      .user(null)
      .item(null)
      .build();

    assertFalse(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterNegation)
    );
  }

  @Test
  void evaluateConditionalFilters() {
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
    assertFalse(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterCondition)
    );

    // testing OR filter
    filterCondition.setOperation(BursarExportFilterCondition.OperationEnum.OR);
    assertThat(
      BursarFilterEvaluator.evaluate(accountWithAncillaryData, filterCondition),
      is(true)
    );
  }
}
