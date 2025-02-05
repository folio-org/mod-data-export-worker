package org.folio.dew.config.feign;

import static feign.FeignException.errorStatus;
import static org.folio.dew.utils.Constants.CANNOT_GET_RECORD_FROM_INVENTORY;

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
    } else if (HttpStatus.INTERNAL_SERVER_ERROR.value() == response.status()) {
      return new BulkEditException(CANNOT_GET_RECORD_FROM_INVENTORY.formatted(requestUrl, response.reason()), ErrorType.ERROR);
    }
    return errorStatus(methodKey, response);
  }

}
