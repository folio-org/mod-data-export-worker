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

import javax.annotation.CheckForNull;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.BursarExportDataToken;
import org.folio.dew.domain.dto.BursarExportTokenConstant;
import org.folio.dew.domain.dto.BursarExportTokenDate;
import org.folio.dew.domain.dto.BursarExportTokenFeeAmount;
import org.folio.dew.domain.dto.BursarExportTokenLengthControl;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;

@Log4j2
@UtilityClass
public class BursarTokenFormatter {

  public static String formatDataToken(
    BursarExportDataToken token,
    AccountWithAncillaryData account
  ) {
    if (token instanceof BursarExportTokenConstant) {
      BursarExportTokenConstant tokenConstant = (BursarExportTokenConstant) token;
      return tokenConstant.getValue();
    } else if (token instanceof BursarExportTokenDate){
      BursarExportTokenDate tokenDate = (BursarExportTokenDate) token;
      String result;

      ZonedDateTime currentDateTime;

      try {
        currentDateTime = Instant.now().atZone(ZoneId.of(tokenDate.getTimezone()));
      } catch (ZoneRulesException e){
        result = String.format("[unknown time zone: %s]", tokenDate.getTimezone());
        return applyLengthControl(
          result,
          tokenDate.getLengthControl()
        );
      }

      switch (tokenDate.getValue()){
        case YEAR_LONG:
          result = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy"));
          break;
        case YEAR_SHORT:
          result = currentDateTime.format(DateTimeFormatter.ofPattern("yy"));
          break;
        case MONTH:
          result = String.valueOf(currentDateTime.getMonthValue());
          break;
        case DATE:
          result = String.valueOf(currentDateTime.getDayOfMonth());
          break;
        case HOUR:
          result = String.valueOf(currentDateTime.getHour());
          break;
        case MINUTE:
          result = String.valueOf(currentDateTime.getMinute());
          break;
        case SECOND:
          result = String.valueOf(currentDateTime.getSecond());
          break;
        case QUARTER:
          result = currentDateTime.format(DateTimeFormatter.ofPattern("Q"));
          break;
        case WEEK_OF_YEAR:
          result = String.valueOf(currentDateTime.get(WeekFields.of(DayOfWeek.MONDAY, 7).weekOfYear()));
          break;
        case WEEK_OF_YEAR_ISO:
          result = String.valueOf(currentDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
          break;
        case WEEK_YEAR:
          result = currentDateTime.format(DateTimeFormatter.ofPattern("YYYY"));
          break;
        case WEEK_YEAR_ISO:
          result = String.valueOf(currentDateTime.get(IsoFields.WEEK_BASED_YEAR));
          break;
        default:
          result = String.format("[invalid date type %s]", tokenDate.getValue());
      }

      return applyLengthControl(
        result,
        tokenDate.getLengthControl()
      );
    }
    else if (token instanceof BursarExportTokenFeeAmount tokenFeeAmount) {
      String result;

      if (tokenFeeAmount.getDecimal()){
        result = new DecimalFormat("0.00").format(account.getAccount().getAmount());
      } else {
        result = account.getAccount().getAmount().multiply(new BigDecimal("100")).setScale(0).toString();
      }
      return applyLengthControl(
        result,
        tokenFeeAmount.getLengthControl()
      );
    }
    else {
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
}
