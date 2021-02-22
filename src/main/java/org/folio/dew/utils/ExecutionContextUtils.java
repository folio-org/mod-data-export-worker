package org.folio.dew.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.entity.constants.JobParameterNames;
import org.springframework.batch.item.ExecutionContext;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionContextUtils {

  public static String getObjectNameByOutputFilePath(ExecutionContext executionContext) {
    String outputFilePath = executionContext.getString(JobParameterNames.OUTPUT_FILE_PATH);
    return FilenameUtils.getName(outputFilePath);
  }

}
