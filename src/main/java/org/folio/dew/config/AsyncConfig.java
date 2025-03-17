package org.folio.dew.config;

import lombok.extern.log4j.Log4j2;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@Log4j2
public class AsyncConfig {

  private static final int TASK_EXECUTOR_CORE_POOL_SIZE = 10;
  private static final int TASK_EXECUTOR_MAX_POOL_SIZE = 10;
  private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

  @Value("${application.core-pool-size}")
  private int corePoolSize;

  @Value("${application.max-pool-size}")
  private int maxPoolSize;

  @Bean(name = "asyncJobLauncher")
  public JobLauncher getAsyncJobLauncher(
      JobRepository jobRepository, @Qualifier("asyncTaskExecutor") TaskExecutor taskExecutor) {
    var jobLauncher = new TaskExecutorJobLauncher();
    jobLauncher.setJobRepository(jobRepository);
    jobLauncher.setTaskExecutor(taskExecutor);
    return jobLauncher;
  }

  @Bean(name = "asyncTaskExecutor")
  public TaskExecutor getAsyncTaskExecutor() {
    var threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(TASK_EXECUTOR_CORE_POOL_SIZE);
    threadPoolTaskExecutor.setMaxPoolSize(TASK_EXECUTOR_MAX_POOL_SIZE);
    threadPoolTaskExecutor.setTaskDecorator(
      FolioExecutionScopeExecutionContextManager::getRunnableWithCurrentFolioContext);
    return threadPoolTaskExecutor;
  }

  @JobScope
  @Bean(name = "asyncTaskExecutorBulkEdit")
  public TaskExecutor getAsyncTaskExecutorBulkEdit() {
    log.info("asyncTaskExecutorBulkEdit established");
    var threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
    threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
    threadPoolTaskExecutor.setTaskDecorator(
      FolioExecutionScopeExecutionContextManager::getRunnableWithCurrentFolioContext);
    return threadPoolTaskExecutor;
  }

}
