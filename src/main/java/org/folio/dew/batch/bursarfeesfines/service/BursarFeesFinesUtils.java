package org.folio.dew.batch.bursarfeesfines.service;

import java.time.Instant;
import javax.annotation.CheckForNull;
import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.BursarExportTokenLengthControl;

@UtilityClass
public class BursarFeesFinesUtils {

  public static final String GET_FILENAME_STEP = "GET_FILENAME";
  public static final String EXPORT_STEP = "EXPORT_STEP";

  private static final String FILE_PATTERN = "lib_%s.dat";
  private static final String DESCRIPTION_PATTERN = "# of accounts: %d";

  public static String getFilename() {
    return String.format(FILE_PATTERN, Instant.now().toString());
  }

  public static String getJobDescriptionPart() {
    return DESCRIPTION_PATTERN;
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
