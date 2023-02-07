package org.folio.dew.batch.authoritycontrol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.AuthorityControlExportConfig;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class AuthorityControlJobConfig {
  private final ObjectMapper objectMapper;

  @JobScope
  @Bean("authorityControlExportConfig")
  public AuthorityControlExportConfig exportConfig(
    @Value("#{jobParameters['authorityControlExportConfig']}") String exportConfigStr) throws JsonProcessingException {
    return objectMapper.readValue(exportConfigStr, AuthorityControlExportConfig.class);
  }
}
