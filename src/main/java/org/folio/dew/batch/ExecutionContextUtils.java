package org.folio.dew.batch;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionContextUtils {

  public static Object getExecutionVariable(StepExecution stepExecution, String variable) {
    return stepExecution.getJobExecution().getExecutionContext().get(variable);
  }

  public static String getFromJobExecutionContext(JobExecution jobExecution, String key) {
    return jobExecution.getExecutionContext().containsKey(key) ? jobExecution.getExecutionContext().getString(key) : null;
  }

  public static void addToJobExecutionContext(StepExecution stepExecution, String key, String value, String delimiter) {
    var jobExecution = stepExecution.getJobExecution();
    String oldUrl = getFromJobExecutionContext(jobExecution, key);
    jobExecution.getExecutionContext().putString(key, StringUtils.isBlank(oldUrl) ? value : oldUrl + delimiter + value);
  }

}
