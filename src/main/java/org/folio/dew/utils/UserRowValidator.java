package org.folio.dew.utils;

import com.opencsv.exceptions.CsvValidationException;
import com.opencsv.validators.RowValidator;
import org.folio.dew.domain.dto.UserFormat;

public class UserRowValidator implements RowValidator {

  @Override
  public boolean isValid(String[] line) {
    return line.length == UserFormat.getUserColumnHeaders().length();
  }

  @Override
  public void validate(String[] line) throws CsvValidationException {
    if (!isValid(line)) {
      throw new CsvValidationException();
    }
  }
}
