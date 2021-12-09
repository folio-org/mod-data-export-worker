package org.folio.dew.config.feign;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class FeignClientConfiguration {
  @Bean
  public ErrorDecoder errorDecoder() {
    return new CustomFeignErrorDecoder();
  }
}
