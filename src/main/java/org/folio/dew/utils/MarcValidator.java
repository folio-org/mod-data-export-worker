package org.folio.dew.utils;

import static org.folio.dew.utils.Constants.FIXED_FIELD;
import static org.folio.dew.utils.Constants.FIXED_FIELD_LENGTH;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import org.folio.dew.error.MarcValidationException;
import org.marc4j.MarcJsonReader;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.Record;

@UtilityClass
public class MarcValidator {
  public static final String INVALID_MARC_MESSAGE =
      "Underlying MARC record contains invalid data and the record cannot be updated.";

  public static void validate(String marcJsonString) throws MarcValidationException, IOException {
    try (var is = new ByteArrayInputStream(marcJsonString.getBytes(StandardCharsets.UTF_8))) {
      var reader = new MarcJsonReader(is);
      validate(reader.next());
    }
  }

  public static void validate(Record marcRecord) throws MarcValidationException {
    for (ControlField cf : marcRecord.getControlFields()) {
      if (FIXED_FIELD.equals(cf.getTag())) {
        if (cf.getData().length() != FIXED_FIELD_LENGTH) {
          throw new MarcValidationException(INVALID_MARC_MESSAGE);
        }
        break;
      }
    }
  }
}
