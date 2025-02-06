package org.folio.dew.config.feign;

import static org.folio.dew.utils.Constants.CANNOT_GET_RECORD;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.springframework.http.HttpStatus;

public class CustomFeignErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    String requestUrl = response.request().url();
    if (HttpStatus.NOT_FOUND.value() == response.status()) {
      return new NotFoundException(requestUrl);
    }
    String reason = response.reason() != null ? response.reason() : "Unknown error";
    return new BulkEditException(CANNOT_GET_RECORD.formatted(requestUrl, reason), ErrorType.ERROR);
  }

}
