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
import java.util.List;
import javax.annotation.CheckForNull;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.BursarExportDataToken;
import org.folio.dew.domain.dto.BursarExportHeaderFooter;
import org.folio.dew.domain.dto.BursarExportTokenAggregate;
import org.folio.dew.domain.dto.BursarExportTokenConstant;
import org.folio.dew.domain.dto.BursarExportTokenConstantConditional;
import org.folio.dew.domain.dto.BursarExportTokenConstantConditionalConditionsInner;
import org.folio.dew.domain.dto.BursarExportTokenDate;
import org.folio.dew.domain.dto.BursarExportTokenFeeAmount;
import org.folio.dew.domain.dto.BursarExportTokenFeeMetadata;
import org.folio.dew.domain.dto.BursarExportTokenItemData;
import org.folio.dew.domain.dto.BursarExportTokenLengthControl;
import org.folio.dew.domain.dto.BursarExportTokenUserData;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;

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
    } else if (token instanceof BursarExportTokenDate tokenDate) {
      return processDateToken(tokenDate);
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

  private static String formatFeeAmountsDataToken(
    BursarExportTokenFeeAmount tokenFeeAmount,
    AccountWithAncillaryData accountWithAncillaryData
  ) {
    String result;

    if (tokenFeeAmount.getDecimal()) {
      result =
        new DecimalFormat("0.00")
          .format(accountWithAncillaryData.getAccount().getAmount());
    } else {
      result =
        accountWithAncillaryData
          .getAccount()
          .getAmount()
          .multiply(new BigDecimal("100"))
          .setScale(0)
          .toString();
    }
    return applyLengthControl(result, tokenFeeAmount.getLengthControl());
  }

  private static String formatFeeMetadataToken(
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

  private static String formatDateDataToken(BursarExportTokenDate tokenDate) {
    return processDateToken(tokenDate);
  }

  private static String formatUserDataToken(
    BursarExportTokenUserData tokenUserData,
    AccountWithAncillaryData accountWithAncillaryData
  ) {
    String result;
    if (
      tokenUserData.getValue() == BursarExportTokenUserData.ValueEnum.FOLIO_ID
    ) {
      result = accountWithAncillaryData.getUser().getId();
    } else if (
      tokenUserData.getValue() ==
      BursarExportTokenUserData.ValueEnum.PATRON_GROUP_ID
    ) {
      result = accountWithAncillaryData.getUser().getPatronGroup();
    } else {
      result =
        String.format(
          "[unexpected user data token: %s]",
          tokenUserData.getValue()
        );
    }

    return applyLengthControl(result, tokenUserData.getLengthControl());
  }

  private static String formatItemDataToken(
    BursarExportTokenItemData tokenItemData,
    AccountWithAncillaryData accountWithAncillaryData
  ) {
    String result;
    switch (tokenItemData.getValue()) {
      case LOCATION_ID -> result =
        accountWithAncillaryData.getItem().getEffectiveLocation().getId();
      case NAME -> result = accountWithAncillaryData.getItem().getTitle();
      case BARCODE -> result = accountWithAncillaryData.getItem().getBarcode();
      case MATERIAL_TYPE -> result =
        accountWithAncillaryData.getItem().getMaterialType().getName();
      default -> result = tokenItemData.getPlaceholder();
    }

    return applyLengthControl(result, tokenItemData.getLengthControl());
  }

  public static String formatDataToken(
    BursarExportDataToken token,
    AccountWithAncillaryData account
  ) {
    if (token instanceof BursarExportTokenConstant tokenConstant) {
      return tokenConstant.getValue();
    } else if (
      token instanceof BursarExportTokenConstantConditional tokenConstantConditional
    ) {
      return processConstantConditional(tokenConstantConditional, account);
    } else if (token instanceof BursarExportTokenDate tokenDate) {
      return formatDateDataToken(tokenDate);
    } else if (token instanceof BursarExportTokenFeeAmount tokenFeeAmount) {
      return formatFeeAmountsDataToken(tokenFeeAmount, account);
    } else if (token instanceof BursarExportTokenFeeMetadata tokenFeeMetadata) {
      return formatFeeMetadataToken(tokenFeeMetadata, account);
    } else if (token instanceof BursarExportTokenUserData tokenUserData) {
      return formatUserDataToken(tokenUserData, account);
    } else if (token instanceof BursarExportTokenItemData tokenItemData) {
      return formatItemDataToken(tokenItemData, account);
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
      if (
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
  private String processDateToken(BursarExportTokenDate tokenDate) {
    String result;

    ZonedDateTime currentDateTime;

    try {
      currentDateTime =
        Instant.now().atZone(ZoneId.of(tokenDate.getTimezone()));
    } catch (ZoneRulesException e) {
      log.error("Unknown timezone: ", e);
      result =
        String.format("[unknown time zone: %s]", tokenDate.getTimezone());
      return applyLengthControl(result, tokenDate.getLengthControl());
    }

    switch (tokenDate.getValue()) {
      case YEAR_LONG -> result =
        currentDateTime.format(DateTimeFormatter.ofPattern("yyyy"));
      case YEAR_SHORT -> result =
        currentDateTime.format(DateTimeFormatter.ofPattern("yy"));
      case MONTH -> result = String.valueOf(currentDateTime.getMonthValue());
      case DATE -> result = String.valueOf(currentDateTime.getDayOfMonth());
      case HOUR -> result = String.valueOf(currentDateTime.getHour());
      case MINUTE -> result = String.valueOf(currentDateTime.getMinute());
      case SECOND -> result = String.valueOf(currentDateTime.getSecond());
      case QUARTER -> result =
        currentDateTime.format(DateTimeFormatter.ofPattern("Q"));
      case WEEK_OF_YEAR -> result =
        String.valueOf(
          currentDateTime.get(WeekFields.of(DayOfWeek.MONDAY, 7).weekOfYear())
        );
      case WEEK_OF_YEAR_ISO -> result =
        String.valueOf(currentDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
      case WEEK_YEAR -> result =
        currentDateTime.format(DateTimeFormatter.ofPattern("YYYY"));
      case WEEK_YEAR_ISO -> result =
        String.valueOf(currentDateTime.get(IsoFields.WEEK_BASED_YEAR));
      default -> result =
        String.format("[invalid date type %s]", tokenDate.getValue());
    }

    return applyLengthControl(result, tokenDate.getLengthControl());
  }

  /*
   * Helper method to process aggregate token into string
   * @param tokenAggregate aggregate token that needs to process into string
   */
  private String processAggregateToken(
    BursarExportTokenAggregate tokenAggregate,
    int aggregateNumRows,
    BigDecimal aggregateTotalAmount
  ) {
    String result;

    switch (tokenAggregate.getValue()) {
      case NUM_ROWS -> result = String.valueOf(aggregateNumRows);
      case TOTAL_AMOUNT -> result =
        aggregateTotalAmount
          .multiply(new BigDecimal("100"))
          .setScale(0)
          .toString();
      default -> result =
        String.format("[invalid aggregate type %s]", tokenAggregate.getValue());
    }

    return applyLengthControl(result, tokenAggregate.getLengthControl());
  }

  /*
   * Helper method to process conditional constant token
   * @param tokenConstantConditional conditional constant token
   */
  private String processConstantConditional(
    BursarExportTokenConstantConditional tokenConstantConditional,
    AccountWithAncillaryData account
  ) {
    List<BursarExportTokenConstantConditionalConditionsInner> conditions = tokenConstantConditional.getConditions();

    for (BursarExportTokenConstantConditionalConditionsInner condition : conditions) {
      if (BursarFilterEvaluator.evaluate(account, condition.getCondition())) {
        return condition.getValue().getValue();
      }
    }

    return tokenConstantConditional.getElse().getValue();
  }
}
