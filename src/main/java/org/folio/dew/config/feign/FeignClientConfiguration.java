package org.folio.dew.config.feign;

import feign.Contract;
import feign.codec.ErrorDecoder;
import java.util.ArrayList;
import java.util.List;
import org.folio.dew.client.NotesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.format.support.FormattingConversionService;

public class FeignClientConfiguration {

  @Autowired(required = false)
  private List<AnnotatedParameterProcessor> parameterProcessors = new ArrayList<>();

  @Bean
  public ErrorDecoder errorDecoder() {
    return new CustomFeignErrorDecoder();
  }

  @Bean
  public Contract feignContract(FormattingConversionService feignConversionService) {
    feignConversionService.addConverter(new NotesClient.NoteLinkTypeToPathVarConverter());
    feignConversionService.addConverter(new NotesClient.NoteLinkDomainToPathVarConverter());
    return new SpringMvcContract(this.parameterProcessors, feignConversionService);
  }
}
