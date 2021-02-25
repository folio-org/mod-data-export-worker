package org.folio.dew.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DataConfig {
  private static final int MAX_DATA_SOURCE_POOL_SIZE = 20;

  @Bean
  public JobRepository getJobRepository(
      @Qualifier("jobRepositoryDataSource") DataSource dataSource,
      PlatformTransactionManager transactionManager)
      throws Exception {
    JobRepositoryFactoryBean jobRepositoryFactory = new JobRepositoryFactoryBean();
    jobRepositoryFactory.setDataSource(dataSource);
    jobRepositoryFactory.setTransactionManager(transactionManager);
    jobRepositoryFactory.setIsolationLevelForCreate("ISOLATION_SERIALIZABLE");
    jobRepositoryFactory.setTablePrefix("BATCH_");
    jobRepositoryFactory.setMaxVarCharLength(1000);

    return jobRepositoryFactory.getObject();
  }

  @Bean("jobRepositoryDataSource")
  public DataSource getJobRepositoryDataSource(
      @Value("${dataSource.driver}") String driverClassName,
      @Value("${dataSource.jdbcUrl}") String jdbcUrl,
      @Value("${dataSource.username}") String userName,
      @Value("${dataSource.password}") String password) {

    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setDriverClassName(driverClassName);
    dataSource.setJdbcUrl(jdbcUrl);
    dataSource.setUsername(userName);
    dataSource.setPassword(password);
    dataSource.setMaximumPoolSize(MAX_DATA_SOURCE_POOL_SIZE);

    return dataSource;
  }

}
