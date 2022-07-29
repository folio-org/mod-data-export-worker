package org.folio.dew.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.folio.dew.config.properties.DataSourceProperties;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchDataConfig {

  @Bean
  public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
    JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
    postProcessor.setJobRegistry(jobRegistry);
    return postProcessor;
  }

  @Bean
  public JobRepository getJobRepository(
      @Qualifier("jobRepositoryDataSource") DataSource dataSource,
      PlatformTransactionManager transactionManager) throws Exception {
    var jobRepositoryFactory = new JobRepositoryFactoryBean();
    jobRepositoryFactory.setDataSource(dataSource);
    jobRepositoryFactory.setTransactionManager(transactionManager);
    jobRepositoryFactory.setIsolationLevelForCreate("ISOLATION_SERIALIZABLE");
    jobRepositoryFactory.setTablePrefix("BATCH_");
    jobRepositoryFactory.setMaxVarCharLength(1000);

    return jobRepositoryFactory.getObject();
  }

  @Bean("jobRepositoryDataSource")
  public DataSource getJobRepositoryDataSource(DataSourceProperties properties) {
    var dataSource = new HikariDataSource();
    dataSource.setDriverClassName(properties.getDriver());
    dataSource.setJdbcUrl(properties.getJdbcUrl());
    dataSource.setUsername(properties.getUsername());
    dataSource.setPassword(properties.getPassword());
    dataSource.setMaximumPoolSize(properties.getMaxPoolSize());

    return dataSource;
  }

}
