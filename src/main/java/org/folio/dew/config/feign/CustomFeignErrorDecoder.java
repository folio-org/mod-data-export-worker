package org.folio.dew.config.feign;

import static feign.FeignException.errorStatus;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.folio.dew.error.NotFoundException;
import org.springframework.http.HttpStatus;

public class CustomFeignErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    String requestUrl = response.request().url();
    if (HttpStatus.NOT_FOUND.value() == response.status()) {
      return new NotFoundException(requestUrl);
    }
    return errorStatus(methodKey, response);
  }
}
