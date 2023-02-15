package org.folio.dew.utils;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.folio.dew.utils.Constants.LINE_BREAK;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Objects;
import java.util.stream.Collectors;

@UtilityClass
public class ExceptionHelper {
  public static String fetchMessage(Throwable throwable) {
    var cause = ExceptionUtils.getRootCause(throwable);
    if (cause instanceof InvalidFormatException) {
      var ife = (InvalidFormatException) cause;
      var path = ife.getPath().stream().map(JsonMappingException.Reference::getFieldName).filter(Objects::nonNull).collect(Collectors.joining("."));
      return String.format("Failed to parse %s from value \"%s\" in %s", ife.getTargetType().getSimpleName(), ife.getValue(), path);
    }
    return ExceptionUtils.getRootCauseMessage(throwable).replace(LINE_BREAK, SPACE);
  }
}
