package org.folio.dew.config.feign;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Encoder;
import feign.jackson.JacksonEncoder;
import org.springframework.context.annotation.Bean;

public class FeignEncoderConfiguration {
  @Bean
  public Encoder feignEncoder() {
    return new JacksonEncoder(customFeignObjectMapper());
  }

  public ObjectMapper customFeignObjectMapper(){
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return objectMapper;
  }
}
