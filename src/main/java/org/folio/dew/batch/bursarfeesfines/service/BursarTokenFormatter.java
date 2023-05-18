package org.folio.dew.batch.bursarfeesfines.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.time.zone.ZoneRulesException;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.BursarExportDataToken;
import org.folio.dew.domain.dto.BursarExportHeaderFooter;
import org.folio.dew.domain.dto.BursarExportTokenAggregate;
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
import org.folio.dew.domain.dto.BursarExportTokenUserData;
import org.folio.dew.domain.dto.BursarExportTokenUserDataOptional;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;

@Log4j2
@UtilityClass
public class BursarTokenFormatter {

  public static String formatHeaderFooterToken(
    BursarExportHeaderFooter token,
    int aggregateNumRows,
    BigDecimal aggregateTotalAmount
  ) {
    if (token instanceof BursarExportTokenConstant tokenConstant) {
      return tokenConstant.getValue();
    } else if (token instanceof BursarExportTokenCurrentDate tokenDate) {
      return formatCurrentDateDataToken(tokenDate);
    } else if (token instanceof BursarExportTokenAggregate tokenAggregate) {
      return processAggregateToken(
        tokenAggregate,
        aggregateNumRows,
        aggregateTotalAmount
      );
    } else {
      log.error("Unexpected token: ", token);
      return String.format("[header/footer-placeholder %s]", token.getType());
    }
  }

  public static String formatFeeAmountToken(
    BursarExportTokenFeeAmount tokenFeeAmount,
    BigDecimal feeAmount
  ) {
    String result;

    if (tokenFeeAmount.getDecimal()) {
      result = new DecimalFormat("0.00").format(feeAmount);
    } else {
      result = feeAmount.multiply(new BigDecimal("100")).setScale(0).toString();
    }

    return applyLengthControl(result, tokenFeeAmount.getLengthControl());
  }

  public static String formatFeeMetaDataToken(
    BursarExportTokenFeeMetadata tokenFeeMetadata,
    AccountWithAncillaryData accountWithAncillaryData
  ) {
    if (
      tokenFeeMetadata.getValue() == BursarExportTokenFeeMetadata.ValueEnum.ID
    ) {
      return accountWithAncillaryData.getAccount().getFeeFineId();
    } else if (
      tokenFeeMetadata.getValue() == BursarExportTokenFeeMetadata.ValueEnum.NAME
    ) {
      return accountWithAncillaryData.getAccount().getFeeFineType();
    } else {
      return String.format(
        "[unexpected metadata token: %s]",
        tokenFeeMetadata.getValue()
      );
    }
  }

  public static String formatCurrentDateDataToken(
    BursarExportTokenCurrentDate tokenDate
  ) {
    ZonedDateTime currentDateTime;

    try {
      currentDateTime =
        Instant.now().atZone(ZoneId.of(tokenDate.getTimezone()));
    } catch (ZoneRulesException e) {
      log.error("Unknown timezone: ", e);
      String result = String.format(
        "[unknown time zone: %s]",
        tokenDate.getTimezone()
      );
      return applyLengthControl(result, tokenDate.getLengthControl());
    }
    return processDateToken(
      currentDateTime,
      tokenDate.getValue(),
      tokenDate.getLengthControl()
    );
  }

  public static String formatFeeDateDataToken(
    BursarExportTokenFeeDate tokenFeeDate,
    AccountWithAncillaryData accountWithAncillaryData
  ) {
    ZonedDateTime feeDateTime;
    Date accountDate;

    switch (tokenFeeDate.getProperty()) {
      case CREATED -> accountDate =
        accountWithAncillaryData.getAccount().getDateCreated();
      case UPDATED -> accountDate =
        accountWithAncillaryData.getAccount().getDateUpdated();
      case DUE -> accountDate =
        accountWithAncillaryData.getAccount().getDueDate();
      case RETURNED -> accountDate =
        accountWithAncillaryData.getAccount().getReturnedDate();
      default -> {
        log.error("[invalid fee date property: {}]", tokenFeeDate.getValue());
        return tokenFeeDate.getPlaceholder();
      }
    }

    try {
      feeDateTime =
        accountDate.toInstant().atZone(ZoneId.of(tokenFeeDate.getTimezone()));
    } catch (ZoneRulesException e) {
      log.error("Unknown timezone: ", e);
      String result = String.format(
        "[unknown time zone: %s]",
        tokenFeeDate.getTimezone()
      );
      return applyLengthControl(result, tokenFeeDate.getLengthControl());
    }

    return processDateToken(
      feeDateTime,
      tokenFeeDate.getValue(),
      tokenFeeDate.getLengthControl()
    );
  }

  public static String formatUserDataToken(
    BursarExportTokenUserData tokenUserData,
    User user
  ) {
    String result;
    switch (tokenUserData.getValue()) {
      case FOLIO_ID -> result = user.getId();
      case PATRON_GROUP_ID -> result = user.getPatronGroup();
      case EXTERNAL_SYSTEM_ID -> result = user.getExternalSystemId();
      default -> result =
        String.format(
          "[unexpected user data token: %s]",
          tokenUserData.getValue()
        );
    }

    return applyLengthControl(result, tokenUserData.getLengthControl());
  }

  public static String formatUserDataOptionalToken(
    BursarExportTokenUserDataOptional tokenUserData,
    User user
  ) {
    String result;
    switch (tokenUserData.getValue()) {
      case BARCODE -> result = user.getBarcode();
      case USERNAME -> result = user.getUsername();
      case FIRST_NAME -> result = user.getPersonal().getFirstName();
      case MIDDLE_NAME -> result = user.getPersonal().getMiddleName();
      case LAST_NAME -> result = user.getPersonal().getLastName();
      default -> {
        log.error("Invalid user data token: {}", tokenUserData);
        result = tokenUserData.getPlaceholder();
      }
    }

    if (result == null) {
      result = tokenUserData.getPlaceholder();
    }

    return applyLengthControl(result, tokenUserData.getLengthControl());
  }

  public static String formatItemDataToken(
    BursarExportTokenItemData tokenItemData,
    AccountWithAncillaryData accountWithAncillaryData
  ) {
    if (accountWithAncillaryData.getItem() == null) {
      return applyLengthControl(
        tokenItemData.getPlaceholder(),
        tokenItemData.getLengthControl()
      );
    }

    String result;
    switch (tokenItemData.getValue()) {
      case LOCATION_ID -> result =
        accountWithAncillaryData.getItem().getEffectiveLocation().getId();
      case NAME -> result = accountWithAncillaryData.getItem().getTitle();
      case BARCODE -> result = accountWithAncillaryData.getItem().getBarcode();
      case MATERIAL_TYPE -> result =
        accountWithAncillaryData.getItem().getMaterialType().getName();
      default -> {
        log.error("Invalid item data token: {}", tokenItemData);
        result = tokenItemData.getPlaceholder();
      }
    }

    if (result == null) {
      result = tokenItemData.getPlaceholder();
    }

    return applyLengthControl(result, tokenItemData.getLengthControl());
  }

  public static String formatConditionalDataToken(
    BursarExportTokenConditional tokenConditional,
    AccountWithAncillaryData account
  ) {
    List<BursarExportTokenConditionalConditionsInner> conditions = tokenConditional.getConditions();

    for (BursarExportTokenConditionalConditionsInner condition : conditions) {
      if (BursarFilterEvaluator.evaluate(account, condition.getCondition())) {
        return formatDataToken(condition.getValue(), account);
      }
    }

    return formatDataToken(tokenConditional.getElse(), account);
  }

  public static String formatDataToken(
    BursarExportDataToken token,
    AccountWithAncillaryData account
  ) {
    if (token instanceof BursarExportTokenConstant tokenConstant) {
      return tokenConstant.getValue();
    } else if (token instanceof BursarExportTokenConditional tokenConditional) {
      return formatConditionalDataToken(tokenConditional, account);
    } else if (token instanceof BursarExportTokenCurrentDate tokenDate) {
      return formatCurrentDateDataToken(tokenDate);
    } else if (token instanceof BursarExportTokenFeeDate tokenFeeDate) {
      return formatFeeDateDataToken(tokenFeeDate, account);
    } else if (token instanceof BursarExportTokenFeeAmount tokenFeeAmount) {
      return formatFeeAmountToken(
        tokenFeeAmount,
        account.getAccount().getAmount()
      );
    } else if (token instanceof BursarExportTokenFeeMetadata tokenFeeMetadata) {
      return formatFeeMetaDataToken(tokenFeeMetadata, account);
    } else if (token instanceof BursarExportTokenUserData tokenUserData) {
      return formatUserDataToken(tokenUserData, account.getUser());
    } else if (token instanceof BursarExportTokenUserDataOptional tokenUserOp) {
      return formatUserDataOptionalToken(tokenUserOp, account.getUser());
    } else if (token instanceof BursarExportTokenItemData tokenItemData) {
      return formatItemDataToken(tokenItemData, account);
    } else {
      log.error("Unexpected token: ", token);
      return String.format("[placeholder %s]", token.getType());
    }
  }

  public static String formatConditionalAggregatedAccountsToken(
    BursarExportTokenConditional tokenConditional,
    AggregatedAccountsByUser aggregatedAccounts
  ) {
    List<BursarExportTokenConditionalConditionsInner> conditions = tokenConditional.getConditions();

    for (BursarExportTokenConditionalConditionsInner condition : conditions) {
      if (
        BursarFilterAggregateEvaluator.evaluate(
          aggregatedAccounts,
          condition.getCondition()
        )
      ) {
        return formatAggregatedAccountsToken(
          condition.getValue(),
          aggregatedAccounts
        );
      }
    }

    return formatAggregatedAccountsToken(
      tokenConditional.getElse(),
      aggregatedAccounts
    );
  }

  public static String formatAggregatedAccountsToken(
    BursarExportDataToken token,
    AggregatedAccountsByUser aggregatedAccounts
  ) {
    if (token instanceof BursarExportTokenConstant tokenConstant) {
      return tokenConstant.getValue();
    } else if (
      token instanceof BursarExportTokenConditional tokenConstantConditional
    ) {
      return formatConditionalAggregatedAccountsToken(
        tokenConstantConditional,
        aggregatedAccounts
      );
    } else if (token instanceof BursarExportTokenAggregate tokenAggregate) {
      return processAggregateToken(
        tokenAggregate,
        aggregatedAccounts.getAccounts().size(),
        aggregatedAccounts.findTotalAmount()
      );
    } else if (token instanceof BursarExportTokenCurrentDate tokenDate) {
      return formatCurrentDateDataToken(tokenDate);
    } else if (token instanceof BursarExportTokenFeeAmount tokenFeeAmount) {
      return formatFeeAmountToken(
        tokenFeeAmount,
        aggregatedAccounts.findTotalAmount()
      );
    } else if (token instanceof BursarExportTokenUserData tokenUserData) {
      return formatUserDataToken(tokenUserData, aggregatedAccounts.getUser());
    } else if (token instanceof BursarExportTokenUserDataOptional tokenUserOp) {
      return formatUserDataOptionalToken(
        tokenUserOp,
        aggregatedAccounts.getUser()
      );
    } else {
      log.error("Unexpected token: ", token);
      return String.format("[placeholder %s]", token.getType());
    }
  }

  public static String applyLengthControl(
    String input,
    @CheckForNull BursarExportTokenLengthControl lengthControl
  ) {
    if (lengthControl == null) {
      return input;
    }

    if (input.length() == lengthControl.getLength()) {
      return input;
    }

    // should be shortened
    if (input.length() > lengthControl.getLength()) {
      if (Boolean.FALSE.equals(lengthControl.getTruncate())) {
        // should not be truncated
        return input;
      } else if (
        lengthControl.getDirection() ==
        BursarExportTokenLengthControl.DirectionEnum.BACK
      ) {
        // truncate from the right
        return input.substring(0, lengthControl.getLength());
      } else {
        // truncate from the left
        return input.substring(input.length() - lengthControl.getLength());
      }
    } else {
      // should be lengthened
      StringBuilder builder = new StringBuilder(lengthControl.getLength());
      if (
        lengthControl.getDirection() ==
        BursarExportTokenLengthControl.DirectionEnum.BACK
      ) {
        builder.append(input);
        builder.append(
          lengthControl
            .getCharacter()
            .repeat(lengthControl.getLength() - input.length())
        );
      } else {
        builder.append(
          lengthControl
            .getCharacter()
            .repeat(lengthControl.getLength() - input.length())
        );
        builder.append(input);
      }
      return builder.toString();
    }
  }

  /*
   * Helper method to process date token into string
   * @params tokenDate date token that needs to process into string
   */
  public static String processDateToken(
    ZonedDateTime dateTime,
    BursarExportTokenDateType dateType,
    BursarExportTokenLengthControl lengthControl
  ) {
    String result;

    switch (dateType) {
      case YEAR_LONG -> result =
        dateTime.format(DateTimeFormatter.ofPattern("yyyy"));
      case YEAR_SHORT -> result =
        dateTime.format(DateTimeFormatter.ofPattern("yy"));
      case MONTH -> result = String.valueOf(dateTime.getMonthValue());
      case DATE -> result = String.valueOf(dateTime.getDayOfMonth());
      case HOUR -> result = String.valueOf(dateTime.getHour());
      case MINUTE -> result = String.valueOf(dateTime.getMinute());
      case SECOND -> result = String.valueOf(dateTime.getSecond());
      case QUARTER -> result =
        dateTime.format(DateTimeFormatter.ofPattern("Q"));
      case WEEK_OF_YEAR -> result =
        String.valueOf(
          dateTime.get(WeekFields.of(DayOfWeek.MONDAY, 7).weekOfYear())
        );
      case WEEK_OF_YEAR_ISO -> result =
        String.valueOf(dateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
      case WEEK_YEAR -> result =
        dateTime.format(DateTimeFormatter.ofPattern("YYYY"));
      case WEEK_YEAR_ISO -> result =
        String.valueOf(dateTime.get(IsoFields.WEEK_BASED_YEAR));
      default -> result = String.format("[invalid date type %s]", dateType);
    }

    return applyLengthControl(result, lengthControl);
  }

  /*
   * Helper method to process aggregate token into string
   * @param tokenAggregate aggregate token that needs to process into string
   */
  public static String processAggregateToken(
    BursarExportTokenAggregate tokenAggregate,
    int aggregateNumRows,
    BigDecimal aggregateTotalAmount
  ) {
    String result;

    switch (tokenAggregate.getValue()) {
      case NUM_ROWS:
        result = String.valueOf(aggregateNumRows);
        break;
      case TOTAL_AMOUNT:
        if (tokenAggregate.getDecimal()) {
          result = new DecimalFormat("0.00").format(aggregateTotalAmount);
        } else {
          result =
            aggregateTotalAmount
              .multiply(new BigDecimal("100"))
              .setScale(0)
              .toString();
        }
        break;
      default:
        result =
          String.format(
            "[invalid aggregate type %s]",
            tokenAggregate.getValue()
          );
    }

    return applyLengthControl(result, tokenAggregate.getLengthControl());
  }
}
