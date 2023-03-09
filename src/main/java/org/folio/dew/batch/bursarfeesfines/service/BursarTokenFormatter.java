package org.folio.dew.batch.bursarfeesfines.service;

import java.time.Instant;
import javax.annotation.CheckForNull;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.BursarExportDataToken;
import org.folio.dew.domain.dto.BursarExportTokenConstant;
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
}
