package org.folio.dew.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@UtilityClass
@Log4j2
public class DateTimeHelper {

  public static Date convertToDate(LocalDateTime dateTime) {
    return dateTime == null ? null : Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

}
