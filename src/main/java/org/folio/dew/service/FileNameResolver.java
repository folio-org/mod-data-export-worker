package org.folio.dew.service;

import static org.folio.dew.utils.Constants.MATCHED_RECORDS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import org.folio.de.entity.JobCommand;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.ExportType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileNameResolver {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSSS");
  @Autowired
  private ObjectMapper objectMapper;

  private final Map<ExportType, BiFunction<JobCommand, String, String>> resolvers = Map.of(
    ExportType.BULK_EDIT_QUERY, bulkEditResolver(),
    ExportType.E_HOLDINGS, eHoldingsResolver()
  );

  public String resolve(JobCommand jobCommand, String workDir, String jobId) {
    Date now = new Date();
    return Optional.ofNullable(resolvers.get(jobCommand.getExportType()))
      .map(resolver -> resolver.apply(jobCommand, workDir))
      .orElse(String.format("%s%s_%tF_%tT_%s", workDir, jobCommand.getExportType(), now, now, jobId));
  }

  private BiFunction<JobCommand, String, String> eHoldingsResolver() {
    return (jobCommand, workDir) -> {
      Date now = new Date();
      try {
        var exportConfigStr = jobCommand.getJobParameters().getString("eHoldingsExportConfig");
        var config = objectMapper.readValue(exportConfigStr, EHoldingsExportConfig.class);
        var recordId = config.getRecordId();
        String fileSuffix;
        if (config.getRecordType() == EHoldingsExportConfig.RecordTypeEnum.RESOURCE) {
          fileSuffix = String.format("%s_resource", recordId);
        } else {
          fileSuffix = String.format("%s_package", recordId);
        }
        return String.format("%s%s_%s", workDir, DATE_FORMAT.format(now), fileSuffix);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(e);
      }
    };
  }

  private BiFunction<JobCommand, String, String> bulkEditResolver() {
    return (jobCommand, workDir) -> workDir + LocalDate.now() + MATCHED_RECORDS + "query";
  }
}
