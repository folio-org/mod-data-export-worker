package org.folio.utils;

import org.apache.commons.io.FilenameUtils;
import org.folio.model.entities.constants.JobParameterNames;
import org.springframework.batch.item.ExecutionContext;

public class ExecutionContextUtils {

    public static String getObjectNameByOutputFilePath(ExecutionContext executionContext) {
        String outputFilePath = executionContext.getString(JobParameterNames.OUTPUT_FILE_PATH);
        String objectName = FilenameUtils.getName(outputFilePath);
        return objectName;
    }
}
