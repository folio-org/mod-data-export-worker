package org.folio.dew.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.folio.des.domain.entity.constant.JobParameterNames;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionContextUtils {

  public static String getObjectNameByOutputFilePath(ExecutionContext executionContext) {
    String outputFilePath = executionContext.getString(JobParameterNames.OUTPUT_FILE_PATH);
    return FilenameUtils.getName(outputFilePath);
  }

  public static Object getExecutionVariable(StepExecution stepExecution, String variable) {
    JobExecution jobExecution = stepExecution.getJobExecution();
    ExecutionContext jobContext = jobExecution.getExecutionContext();
    return jobContext.get(variable);
  }

}
