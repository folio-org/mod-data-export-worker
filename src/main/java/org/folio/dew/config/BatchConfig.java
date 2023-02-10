package org.folio.dew.config;

import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class BatchConfig extends DefaultBatchConfiguration {

  @Bean
  public JobRepository createJobRepository(DataSource dataSource) throws Exception {
    JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
    factory.setDataSource(dataSource);
    factory.setTransactionManager(super.getTransactionManager());
    factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
    factory.setTablePrefix("BATCH_");
    factory.setMaxVarCharLength(2500);
    return factory.getObject();
  }
}
