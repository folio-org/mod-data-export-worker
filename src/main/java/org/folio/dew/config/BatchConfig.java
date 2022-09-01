package org.folio.dew.config;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.batch.JpaBatchConfigurer;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
public class BatchConfig {

  @Bean
  public BatchConfigurer batchConfigurer(BatchProperties properties, DataSource dataSource,
                                         TransactionManagerCustomizers transactionManagerCustomizers,
                                         EntityManagerFactory entityManagerFactory) {
    return new JpaBatchConfigurer (properties, dataSource, transactionManagerCustomizers, entityManagerFactory) {

      @Override
      protected JobRepository createJobRepository() throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(super.getTransactionManager());
        factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        factory.setTablePrefix("BATCH_");
        factory.setMaxVarCharLength(2500);
        return factory.getObject();
      }
    };
  }
}
