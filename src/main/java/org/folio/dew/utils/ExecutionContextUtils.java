package org.folio.dew.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionContextUtils {

  public static Object getExecutionVariable(StepExecution stepExecution, String variable) {
    JobExecution jobExecution = stepExecution.getJobExecution();
    ExecutionContext jobContext = jobExecution.getExecutionContext();
    return jobContext.get(variable);
  }

  public static void addToJobExecutionContext(StepExecution stepExecution, String key, String value) {
    ExecutionContext jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();
    String oldUrl = jobExecutionContext.getString(key);
    jobExecutionContext.putString(key, StringUtils.isBlank(oldUrl) ? value : oldUrl + ';' + value);
  }

}
