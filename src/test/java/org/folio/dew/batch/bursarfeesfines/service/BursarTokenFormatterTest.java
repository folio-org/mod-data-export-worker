package org.folio.dew.batch.bursarfeesfines.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportFilterAmount;
import org.folio.dew.domain.dto.BursarExportFilterAmount.ConditionEnum;
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.BursarExportFilterPatronGroup;
import org.folio.dew.domain.dto.BursarExportTokenAggregate;
import org.folio.dew.domain.dto.BursarExportTokenAggregate.ValueEnum;
import org.folio.dew.domain.dto.BursarExportTokenConditional;
import org.folio.dew.domain.dto.BursarExportTokenConditionalConditionsInner;
import org.folio.dew.domain.dto.BursarExportTokenConstant;
import org.folio.dew.domain.dto.BursarExportTokenCurrentDate;
import org.folio.dew.domain.dto.BursarExportTokenDateType;
import org.folio.dew.domain.dto.BursarExportTokenFeeAmount;
import org.folio.dew.domain.dto.BursarExportTokenFeeDate;
import org.folio.dew.domain.dto.BursarExportTokenFeeMetadata;
import org.folio.dew.domain.dto.BursarExportTokenItemData;
import org.folio.dew.domain.dto.BursarExportTokenLengthControl;
import org.folio.dew.domain.dto.BursarExportTokenLengthControl.DirectionEnum;
import org.folio.dew.domain.dto.BursarExportTokenUserData;
import org.folio.dew.domain.dto.BursarExportTokenUserDataOptional;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemLocation;
import org.folio.dew.domain.dto.MaterialType;
import org.folio.dew.domain.dto.Personal;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BursarTokenFormatterTest {

  public static AccountWithAncillaryData accountWithAncillaryData;
  public static Account account;
  public static User user;
  public static Item item;

  @BeforeAll
  static void setUp() {
    account = new Account();
    account.setAmount(BigDecimal.valueOf(123.45));
    account.setFeeFineId("test_id");
    account.setFeeFineType("test_type");
    account.setDateCreated(
      Date.from(Instant.parse("2023-02-01T00:01:02.000Z"))
    );
    account.setDateUpdated(
      Date.from(Instant.parse("2023-03-01T00:01:02.000Z"))
    );
    account.setDueDate(Date.from(Instant.parse("2023-04-01T00:01:02.000Z")));
    account.setReturnedDate(
      Date.from(Instant.parse("2023-05-01T00:01:02.000Z"))
    );

    user = new User();
    user.setId("test_userid");
    user.setPatronGroup("test_groupid");
    user.setExternalSystemId("test_extid");
    user.setBarcode("test_barcode");
    user.setUsername("test_username");
    Personal personal = new Personal();
    personal.setFirstName("test_firstname");
    personal.setMiddleName("test_middlename");
    personal.setLastName("test_lastname");
    user.setPersonal(personal);

    item = new Item();
    item.setId("test_itemid");
    ItemLocation location = new ItemLocation();
    location.setId("test_locid");
    item.setEffectiveLocation(location);
    item.setTitle("test_title");
    item.setBarcode("test_barcode");
    MaterialType materialType = new MaterialType();
    materialType.setName("test_matname");
    item.setMaterialType(materialType);

    accountWithAncillaryData =
      AccountWithAncillaryData
        .builder()
        .account(account)
        .user(user)
        .item(item)
        .build();
  }

  @Test
  void testApplyLengthControl() {
    String testString = "test";
    BursarExportTokenLengthControl lengthControl = new BursarExportTokenLengthControl();

    // null length control test
    assertThat(
      BursarTokenFormatter.applyLengthControl(testString, null),
      is("test")
    );

    // same length test
    lengthControl.setLength(4);
    assertThat(
      BursarTokenFormatter.applyLengthControl(testString, lengthControl),
      is("test")
    );

    // longer length test
    lengthControl.setLength(8);
    // test front padding
    lengthControl.setCharacter("0");
    lengthControl.setDirection(DirectionEnum.FRONT);
    assertThat(
      BursarTokenFormatter.applyLengthControl(testString, lengthControl),
      is("0000test")
    );
    // test back padding
    lengthControl.setDirection(DirectionEnum.BACK);
    assertThat(
      BursarTokenFormatter.applyLengthControl(testString, lengthControl),
      is("test0000")
    );

    // shorter length test
    lengthControl.setLength(2);
    // test front truncation
    lengthControl.setTruncate(true);
    lengthControl.setDirection(DirectionEnum.FRONT);
    assertThat(
      BursarTokenFormatter.applyLengthControl(testString, lengthControl),
      is("st")
    );
    // test back truncation
    lengthControl.setDirection(DirectionEnum.BACK);
    assertThat(
      BursarTokenFormatter.applyLengthControl(testString, lengthControl),
      is("te")
    );
    // test when truncation is false
    lengthControl.setTruncate(false);
    assertThat(
      BursarTokenFormatter.applyLengthControl(testString, lengthControl),
      is("test")
    );
  }

  @Test
  void testProcessDateToken() {
    ZonedDateTime testDateTime = ZonedDateTime.parse(
      "2023-02-01T00:01:02.000Z"
    );
    BursarExportTokenLengthControl lengthControl = null;

    BursarExportTokenDateType dateType = BursarExportTokenDateType.YEAR_LONG;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("2023")
    );

    dateType = BursarExportTokenDateType.YEAR_SHORT;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("23")
    );

    dateType = BursarExportTokenDateType.MONTH;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("2")
    );

    dateType = BursarExportTokenDateType.DATE;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("1")
    );

    dateType = BursarExportTokenDateType.HOUR;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("0")
    );

    dateType = BursarExportTokenDateType.MINUTE;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("1")
    );

    dateType = BursarExportTokenDateType.SECOND;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("2")
    );

    dateType = BursarExportTokenDateType.QUARTER;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("1")
    );

    dateType = BursarExportTokenDateType.WEEK_OF_YEAR_ISO;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("5")
    );

    dateType = BursarExportTokenDateType.WEEK_YEAR_ISO;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("2023")
    );

    dateType = BursarExportTokenDateType.DAY_OF_YEAR;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("32")
    );

    dateType = BursarExportTokenDateType.YYYYMMDD;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("20230201")
    );

    dateType = BursarExportTokenDateType.YYYY_MM_DD;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("2023-02-01")
    );

    dateType = BursarExportTokenDateType.MMDDYYYY;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("02012023")
    );

    dateType = BursarExportTokenDateType.DDMMYYYY;
    assertThat(
      BursarTokenFormatter.processDateToken(
        testDateTime,
        dateType,
        lengthControl
      ),
      is("01022023")
    );

    assertThat(
      BursarTokenFormatter.processDateToken(null, dateType, lengthControl),
      is("")
    );
  }

  @Test
  void testProcessAggregateToken() {
    int testNumRows = 25;
    BigDecimal testAmount = new BigDecimal("123.45");
    BursarExportTokenAggregate aggregate = new BursarExportTokenAggregate();

    aggregate.setValue(ValueEnum.NUM_ROWS);
    assertThat(
      BursarTokenFormatter.processAggregateToken(
        aggregate,
        testNumRows,
        testAmount
      ),
      is("25")
    );

    aggregate.setValue(ValueEnum.TOTAL_AMOUNT);
    aggregate.setDecimal(true);
    assertThat(
      BursarTokenFormatter.processAggregateToken(
        aggregate,
        testNumRows,
        testAmount
      ),
      is("123.45")
    );
    aggregate.setDecimal(false);
    assertThat(
      BursarTokenFormatter.processAggregateToken(
        aggregate,
        testNumRows,
        testAmount
      ),
      is("12345")
    );
  }

  @Test
  void testFormatHeaderFooterToken() {
    int aggregateNumRows = 25;
    BigDecimal aggregateAmount = new BigDecimal("123.45");

    BursarExportTokenConstant constant = new BursarExportTokenConstant();
    constant.setValue("test");
    assertThat(
      BursarTokenFormatter.formatHeaderFooterToken(
        constant,
        aggregateNumRows,
        aggregateAmount
      ),
      is("test")
    );

    BursarExportTokenCurrentDate currentDate = new BursarExportTokenCurrentDate();
    currentDate.setValue(BursarExportTokenDateType.YEAR_LONG);
    currentDate.setTimezone("UTC");
    String currentYear = String.valueOf(ZonedDateTime.now().getYear());
    assertThat(
      BursarTokenFormatter.formatHeaderFooterToken(
        currentDate,
        aggregateNumRows,
        aggregateAmount
      ),
      is(currentYear)
    );

    BursarExportTokenAggregate aggregate = new BursarExportTokenAggregate();
    aggregate.setValue(ValueEnum.NUM_ROWS);
    assertThat(
      BursarTokenFormatter.formatHeaderFooterToken(
        aggregate,
        aggregateNumRows,
        aggregateAmount
      ),
      is("25")
    );
  }

  @Test
  void testFormatFeeAmountToken() {
    BigDecimal testAmount = new BigDecimal("123.45");
    BursarExportTokenFeeAmount feeAmountToken = new BursarExportTokenFeeAmount();
    feeAmountToken.setLengthControl(null);

    feeAmountToken.setDecimal(true);
    assertThat(
      BursarTokenFormatter.formatFeeAmountToken(feeAmountToken, testAmount),
      is("123.45")
    );
    feeAmountToken.setDecimal(false);
    assertThat(
      BursarTokenFormatter.formatFeeAmountToken(feeAmountToken, testAmount),
      is("12345")
    );
  }

  @Test
  void testFormatFeeMetaDataToken() {
    BursarExportTokenFeeMetadata feeMetadataToken = new BursarExportTokenFeeMetadata();
    feeMetadataToken.setLengthControl(null);
    feeMetadataToken.setValue(BursarExportTokenFeeMetadata.ValueEnum.ID);
    assertThat(
      BursarTokenFormatter.formatFeeMetaDataToken(
        feeMetadataToken,
        accountWithAncillaryData
      ),
      is("test_id")
    );

    feeMetadataToken.setValue(BursarExportTokenFeeMetadata.ValueEnum.NAME);
    assertThat(
      BursarTokenFormatter.formatFeeMetaDataToken(
        feeMetadataToken,
        accountWithAncillaryData
      ),
      is("test_type")
    );
  }

  @Test
  void testFormatCurrentDateDataToken() {
    BursarExportTokenCurrentDate currentDateToken = new BursarExportTokenCurrentDate();
    currentDateToken.setLengthControl(null);
    currentDateToken.setTimezone("UTC");
    currentDateToken.setValue(BursarExportTokenDateType.YEAR_LONG);
    String currentYear = String.valueOf(ZonedDateTime.now().getYear());
    assertThat(
      BursarTokenFormatter.formatCurrentDateDataToken(currentDateToken),
      is(currentYear)
    );

    currentDateToken.setTimezone("invalid");
    assertThat(
      BursarTokenFormatter.formatCurrentDateDataToken(currentDateToken),
      is("[unknown time zone: invalid]")
    );
  }

  @Test
  void testFormatFeeDateDataToken() {
    BursarExportTokenFeeDate feeDateToken = new BursarExportTokenFeeDate();
    feeDateToken.setLengthControl(null);
    feeDateToken.setTimezone("UTC");
    feeDateToken.setValue(BursarExportTokenDateType.MONTH);
    feeDateToken.setProperty(BursarExportTokenFeeDate.PropertyEnum.CREATED);
    feeDateToken.setPlaceholder("");
    assertThat(
      BursarTokenFormatter.formatFeeDateDataToken(
        feeDateToken,
        accountWithAncillaryData
      ),
      is("2")
    );

    feeDateToken.setProperty(BursarExportTokenFeeDate.PropertyEnum.UPDATED);
    assertThat(
      BursarTokenFormatter.formatFeeDateDataToken(
        feeDateToken,
        accountWithAncillaryData
      ),
      is("3")
    );

    feeDateToken.setProperty(BursarExportTokenFeeDate.PropertyEnum.DUE);
    assertThat(
      BursarTokenFormatter.formatFeeDateDataToken(
        feeDateToken,
        accountWithAncillaryData
      ),
      is("4")
    );

    feeDateToken.setProperty(BursarExportTokenFeeDate.PropertyEnum.RETURNED);
    assertThat(
      BursarTokenFormatter.formatFeeDateDataToken(
        feeDateToken,
        accountWithAncillaryData
      ),
      is("5")
    );

    feeDateToken.setTimezone("invalid");
    assertThat(
      BursarTokenFormatter.formatFeeDateDataToken(
        feeDateToken,
        accountWithAncillaryData
      ),
      is("[unknown time zone: invalid]")
    );

    assertThat(
      BursarTokenFormatter.formatFeeDateDataToken(
        feeDateToken,
        accountWithAncillaryData.withAccount(new Account())
      ),
      is("")
    );
  }

  @Test
  void testFormatUserDataToken() {
    BursarExportTokenUserData userDataToken = new BursarExportTokenUserData();
    userDataToken.setLengthControl(null);

    userDataToken.setValue(BursarExportTokenUserData.ValueEnum.FOLIO_ID);
    assertThat(
      BursarTokenFormatter.formatUserDataToken(userDataToken, user),
      is("test_userid")
    );
  }

  @Test
  void testFormatUserDataOptionalToken() {
    BursarExportTokenUserDataOptional tokenUserDataOptional = new BursarExportTokenUserDataOptional();
    tokenUserDataOptional.setLengthControl(null);
    tokenUserDataOptional.setPlaceholder("placeholder");

    tokenUserDataOptional.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.BARCODE
    );
    assertThat(
      BursarTokenFormatter.formatUserDataOptionalToken(
        tokenUserDataOptional,
        user
      ),
      is("test_barcode")
    );

    tokenUserDataOptional.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.USERNAME
    );
    assertThat(
      BursarTokenFormatter.formatUserDataOptionalToken(
        tokenUserDataOptional,
        user
      ),
      is("test_username")
    );

    tokenUserDataOptional.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.FIRST_NAME
    );
    assertThat(
      BursarTokenFormatter.formatUserDataOptionalToken(
        tokenUserDataOptional,
        user
      ),
      is("test_firstname")
    );

    tokenUserDataOptional.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.MIDDLE_NAME
    );
    assertThat(
      BursarTokenFormatter.formatUserDataOptionalToken(
        tokenUserDataOptional,
        user
      ),
      is("test_middlename")
    );

    tokenUserDataOptional.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.LAST_NAME
    );
    assertThat(
      BursarTokenFormatter.formatUserDataOptionalToken(
        tokenUserDataOptional,
        user
      ),
      is("test_lastname")
    );

    // when result is null
    tokenUserDataOptional.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.BARCODE
    );
    assertThat(
      BursarTokenFormatter.formatUserDataOptionalToken(
        tokenUserDataOptional,
        new User()
      ),
      is("placeholder")
    );

    tokenUserDataOptional.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.PATRON_GROUP_ID
    );
    assertThat(
      BursarTokenFormatter.formatUserDataOptionalToken(
        tokenUserDataOptional,
        user
      ),
      is("test_groupid")
    );

    tokenUserDataOptional.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.EXTERNAL_SYSTEM_ID
    );
    assertThat(
      BursarTokenFormatter.formatUserDataOptionalToken(
        tokenUserDataOptional,
        user
      ),
      is("test_extid")
    );
  }

  @Test
  void testFormatItemDataToken() {
    BursarExportTokenItemData itemDataToken = new BursarExportTokenItemData();
    itemDataToken.setLengthControl(null);
    itemDataToken.setPlaceholder("placeholder");

    itemDataToken.setValue(BursarExportTokenItemData.ValueEnum.LOCATION_ID);
    assertThat(
      BursarTokenFormatter.formatItemDataToken(
        itemDataToken,
        accountWithAncillaryData
      ),
      is("test_locid")
    );

    itemDataToken.setValue(BursarExportTokenItemData.ValueEnum.NAME);
    assertThat(
      BursarTokenFormatter.formatItemDataToken(
        itemDataToken,
        accountWithAncillaryData
      ),
      is("test_title")
    );

    itemDataToken.setValue(BursarExportTokenItemData.ValueEnum.BARCODE);
    assertThat(
      BursarTokenFormatter.formatItemDataToken(
        itemDataToken,
        accountWithAncillaryData
      ),
      is("test_barcode")
    );

    itemDataToken.setValue(BursarExportTokenItemData.ValueEnum.MATERIAL_TYPE);
    assertThat(
      BursarTokenFormatter.formatItemDataToken(
        itemDataToken,
        accountWithAncillaryData
      ),
      is("test_matname")
    );

    // test null
    AccountWithAncillaryData tempAccountWithAncillaryData = AccountWithAncillaryData
      .builder()
      .item(null)
      .build();
    itemDataToken.setValue(BursarExportTokenItemData.ValueEnum.NAME);
    assertThat(
      BursarTokenFormatter.formatItemDataToken(
        itemDataToken,
        tempAccountWithAncillaryData
      ),
      is("placeholder")
    );

    tempAccountWithAncillaryData.setItem(new Item());
    itemDataToken.setValue(BursarExportTokenItemData.ValueEnum.NAME);
    assertThat(
      BursarTokenFormatter.formatItemDataToken(
        itemDataToken,
        tempAccountWithAncillaryData
      ),
      is("placeholder")
    );
  }

  @Test
  void testFormatConditionalDataToken() {
    BursarExportTokenConditional conditionalToken = new BursarExportTokenConditional();
    conditionalToken.setElse(
      new BursarExportTokenConstant() {
        {
          setValue("test_else");
        }
      }
    );
    // test else
    assertThat(
      BursarTokenFormatter.formatConditionalDataToken(
        conditionalToken,
        accountWithAncillaryData
      ),
      is("test_else")
    );

    List<BursarExportTokenConditionalConditionsInner> conditions = new ArrayList<>();

    BursarExportTokenConditionalConditionsInner condition1 = new BursarExportTokenConditionalConditionsInner();
    BursarExportTokenFeeAmount feeAmountToken = new BursarExportTokenFeeAmount();
    feeAmountToken.setDecimal(true);
    condition1.setValue(feeAmountToken);
    BursarExportFilterAmount falseAmountFilter = new BursarExportFilterAmount();
    falseAmountFilter.setAmount(100);
    falseAmountFilter.setCondition(ConditionEnum.LESS_THAN);
    condition1.setCondition(falseAmountFilter);
    conditions.add(condition1);

    BursarExportTokenConditionalConditionsInner condition2 = new BursarExportTokenConditionalConditionsInner();
    condition2.setValue(feeAmountToken);
    condition2.setCondition(new BursarExportFilterPass());
    conditions.add(condition2);
    conditionalToken.setConditions(conditions);

    // test conditions
    assertThat(
      BursarTokenFormatter.formatConditionalDataToken(
        conditionalToken,
        accountWithAncillaryData
      ),
      is("123.45")
    );
  }

  @Test
  void testFormatConditionalAggregatedAccountsToken() {
    List<Account> accounts = new ArrayList<Account>();
    for (int i = 0; i < 10; i++) {
      Account account = new Account();
      account.setAmount(new BigDecimal(100));
      accounts.add(account);
    }
    User user = new User();
    user.setPatronGroup("0000-00-00-00-000000");
    AggregatedAccountsByUser aggregatedAccounts = AggregatedAccountsByUser
      .builder()
      .accounts(accounts)
      .user(user)
      .build();

    BursarExportTokenConditional conditionalToken = new BursarExportTokenConditional();
    conditionalToken.setElse(
      new BursarExportTokenConstant() {
        {
          setValue("test_else");
        }
      }
    );

    // test else
    assertThat(
      BursarTokenFormatter.formatConditionalAggregatedAccountsToken(
        conditionalToken,
        aggregatedAccounts
      ),
      is("test_else")
    );

    List<BursarExportTokenConditionalConditionsInner> conditions = new ArrayList<>();
    BursarExportTokenConditionalConditionsInner condition1 = new BursarExportTokenConditionalConditionsInner();
    BursarExportTokenFeeAmount feeAmountToken = new BursarExportTokenFeeAmount();
    feeAmountToken.setDecimal(true);
    condition1.setValue(feeAmountToken);
    BursarExportFilterPatronGroup falsePatronGroupFilter = new BursarExportFilterPatronGroup();
    falsePatronGroupFilter.setPatronGroupId(
      UUID.fromString("0000-00-00-00-000001")
    );
    condition1.setCondition(falsePatronGroupFilter);
    conditions.add(condition1);

    BursarExportTokenConditionalConditionsInner condition2 = new BursarExportTokenConditionalConditionsInner();
    condition2.setValue(feeAmountToken);
    condition2.setCondition(new BursarExportFilterPass());
    conditions.add(condition2);
    conditionalToken.setConditions(conditions);

    assertThat(
      BursarTokenFormatter.formatConditionalAggregatedAccountsToken(
        conditionalToken,
        aggregatedAccounts
      ),
      is("1000.00")
    );
  }

  @Test
  void testFormatDataToken() {
    BursarExportTokenConstant constantToken = new BursarExportTokenConstant();
    constantToken.setValue("test_constant");
    assertThat(
      BursarTokenFormatter.formatDataToken(
        constantToken,
        accountWithAncillaryData
      ),
      is("test_constant")
    );

    BursarExportTokenConditional conditionalToken = new BursarExportTokenConditional();
    conditionalToken.setElse(
      new BursarExportTokenConstant() {
        {
          setValue("test_else");
        }
      }
    );
    assertThat(
      BursarTokenFormatter.formatDataToken(conditionalToken, null),
      is("test_else")
    );

    BursarExportTokenCurrentDate currentDateToken = new BursarExportTokenCurrentDate();
    currentDateToken.setValue(BursarExportTokenDateType.YEAR_LONG);
    currentDateToken.setTimezone("UTC");
    String currentYear = String.valueOf(ZonedDateTime.now().getYear());
    assertThat(
      BursarTokenFormatter.formatDataToken(currentDateToken, null),
      is(currentYear)
    );

    BursarExportTokenFeeDate feeDateToken = new BursarExportTokenFeeDate();
    feeDateToken.setValue(BursarExportTokenDateType.MONTH);
    feeDateToken.setProperty(BursarExportTokenFeeDate.PropertyEnum.CREATED);
    feeDateToken.setTimezone("UTC");
    assertThat(
      BursarTokenFormatter.formatDataToken(
        feeDateToken,
        accountWithAncillaryData
      ),
      is("2")
    );

    BursarExportTokenFeeAmount feeAmountToken = new BursarExportTokenFeeAmount();
    feeAmountToken.setDecimal(true);
    feeAmountToken.setLengthControl(null);
    assertThat(
      BursarTokenFormatter.formatDataToken(
        feeAmountToken,
        accountWithAncillaryData
      ),
      is("123.45")
    );

    BursarExportTokenFeeMetadata feeMetadataToken = new BursarExportTokenFeeMetadata();
    feeMetadataToken.setValue(BursarExportTokenFeeMetadata.ValueEnum.ID);
    feeMetadataToken.setLengthControl(null);
    assertThat(
      BursarTokenFormatter.formatDataToken(
        feeMetadataToken,
        accountWithAncillaryData
      ),
      is("test_id")
    );

    BursarExportTokenUserData userDataToken = new BursarExportTokenUserData();
    userDataToken.setLengthControl(null);
    userDataToken.setValue(BursarExportTokenUserData.ValueEnum.FOLIO_ID);
    assertThat(
      BursarTokenFormatter.formatDataToken(
        userDataToken,
        accountWithAncillaryData
      ),
      is("test_userid")
    );

    BursarExportTokenUserDataOptional userDataOptionalToken = new BursarExportTokenUserDataOptional();
    userDataOptionalToken.setLengthControl(null);
    userDataOptionalToken.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.BARCODE
    );
    assertThat(
      BursarTokenFormatter.formatDataToken(
        userDataOptionalToken,
        accountWithAncillaryData
      ),
      is("test_barcode")
    );

    BursarExportTokenItemData itemDataToken = new BursarExportTokenItemData();
    itemDataToken.setLengthControl(null);
    itemDataToken.setValue(BursarExportTokenItemData.ValueEnum.LOCATION_ID);
    assertThat(
      BursarTokenFormatter.formatDataToken(
        itemDataToken,
        accountWithAncillaryData
      ),
      is("test_locid")
    );

    BursarExportTokenAggregate aggregateToken = new BursarExportTokenAggregate();
    aggregateToken.setType("test");
    assertThat(
      BursarTokenFormatter.formatDataToken(
        aggregateToken,
        accountWithAncillaryData
      ),
      is("[placeholder test]")
    );
  }

  @Test
  void testFormatAggregatedAccountsToken() {
    List<Account> accounts = new ArrayList<Account>();
    for (int i = 0; i < 10; i++) {
      Account account = new Account();
      account.setAmount(new BigDecimal(100));
      accounts.add(account);
    }
    User user = new User();
    user.setId("test_id");
    user.setBarcode("test_barcode");
    AggregatedAccountsByUser aggregatedAccounts = AggregatedAccountsByUser
      .builder()
      .accounts(accounts)
      .user(user)
      .build();

    BursarExportTokenConstant constantToken = new BursarExportTokenConstant();
    constantToken.setValue("test_constant");
    assertThat(
      BursarTokenFormatter.formatAggregatedAccountsToken(
        constantToken,
        aggregatedAccounts
      ),
      is("test_constant")
    );

    BursarExportTokenConditional conditionalToken = new BursarExportTokenConditional();
    conditionalToken.setElse(
      new BursarExportTokenConstant() {
        {
          setValue("test_else");
        }
      }
    );
    assertThat(
      BursarTokenFormatter.formatAggregatedAccountsToken(
        conditionalToken,
        aggregatedAccounts
      ),
      is("test_else")
    );

    BursarExportTokenAggregate aggregateToken = new BursarExportTokenAggregate();
    aggregateToken.setValue(BursarExportTokenAggregate.ValueEnum.TOTAL_AMOUNT);
    aggregateToken.setDecimal(true);
    assertThat(
      BursarTokenFormatter.formatAggregatedAccountsToken(
        aggregateToken,
        aggregatedAccounts
      ),
      is("1000.00")
    );

    BursarExportTokenCurrentDate currentDateToken = new BursarExportTokenCurrentDate();
    currentDateToken.setValue(BursarExportTokenDateType.YEAR_LONG);
    currentDateToken.setTimezone("UTC");
    String currentYear = String.valueOf(ZonedDateTime.now().getYear());
    assertThat(
      BursarTokenFormatter.formatAggregatedAccountsToken(
        currentDateToken,
        aggregatedAccounts
      ),
      is(currentYear)
    );

    BursarExportTokenFeeAmount feeAmountToken = new BursarExportTokenFeeAmount();
    feeAmountToken.setDecimal(true);
    feeAmountToken.setLengthControl(null);
    assertThat(
      BursarTokenFormatter.formatAggregatedAccountsToken(
        feeAmountToken,
        aggregatedAccounts
      ),
      is("1000.00")
    );

    BursarExportTokenUserData userDataToken = new BursarExportTokenUserData();
    userDataToken.setLengthControl(null);
    userDataToken.setValue(BursarExportTokenUserData.ValueEnum.FOLIO_ID);
    assertThat(
      BursarTokenFormatter.formatAggregatedAccountsToken(
        userDataToken,
        aggregatedAccounts
      ),
      is("test_id")
    );

    BursarExportTokenUserDataOptional userDataOptionalToken = new BursarExportTokenUserDataOptional();
    userDataOptionalToken.setLengthControl(null);
    userDataOptionalToken.setValue(
      BursarExportTokenUserDataOptional.ValueEnum.BARCODE
    );
    assertThat(
      BursarTokenFormatter.formatAggregatedAccountsToken(
        userDataOptionalToken,
        aggregatedAccounts
      ),
      is("test_barcode")
    );

    BursarExportTokenItemData itemDataToken = new BursarExportTokenItemData();
    itemDataToken.setLengthControl(null);
    itemDataToken.setType("item data");
    assertThat(
      BursarTokenFormatter.formatAggregatedAccountsToken(
        itemDataToken,
        aggregatedAccounts
      ),
      is("[placeholder item data]")
    );
  }
}
