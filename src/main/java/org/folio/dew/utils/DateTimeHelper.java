package org.folio.dew.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@UtilityClass
@Log4j2
public class DateTimeHelper {

  public static  String formatDate(String catalogedDateInput) {
    if (isEmpty(catalogedDateInput)){
      return EMPTY;
    }

    var inputFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    var outputFormatter = new SimpleDateFormat("yyyy-MM-dd");

    Date date;
    try {
      date = inputFormatter.parse(catalogedDateInput);
    } catch (ParseException e) {
      log.error("Can't parse catalogedDate {}.", catalogedDateInput);
      return catalogedDateInput;
    }
    return outputFormatter.format(date);
  }

}
