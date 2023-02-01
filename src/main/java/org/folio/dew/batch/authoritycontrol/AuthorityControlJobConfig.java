package org.folio.dew.batch.authoritycontrol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.domain.dto.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.ExportType;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class AuthorityControlJobConfig {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final ObjectMapper objectMapper;

  @Bean
  public Job getAuthorityControlJob(
    JobRepository jobRepository,
    JobCompletionNotificationListener jobCompletionNotificationListener,
    @Qualifier("prepareAuthorityControlStep") Step prepareAuthorityControlStep,
    @Qualifier("getAuthorityControlStep") Step getAuthorityControlStep,
    @Qualifier("saveAuthorityControlStep") Step saveAuthorityControlStep,
    @Qualifier("cleanupAuthorityControlStep") Step cleanupAuthorityControlStep) {
    return jobBuilderFactory
      .get(ExportType.E_HOLDINGS.toString())
      .repository(jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .start(prepareAuthorityControlStep)
      .next(getAuthorityControlStep)
      .next(saveAuthorityControlStep)
      .next(cleanupAuthorityControlStep)
      .build();
  }

  @Bean
  @JobScope
  public AuthorityControlExportConfig exportConfig(
    @Value("#{jobParameters['AuthorityControlExportConfig']}") String exportConfigStr) throws JsonProcessingException {
    return objectMapper.readValue(exportConfigStr, AuthorityControlExportConfig.class);
  }
}
