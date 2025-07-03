package org.folio.dew.service;

import static org.folio.dew.utils.Constants.MATCHED_RECORDS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

  private static final String NAME_FORMAT = "%s%s_%s";
  private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSSS");
  @Autowired
  private ObjectMapper objectMapper;

  private final Map<ExportType, BiFunction<JobCommand, String, String>> resolvers = Map.of(
    ExportType.E_HOLDINGS, eHoldingsResolver(),
    ExportType.AUTH_HEADINGS_UPDATES, authHeadingsUpdatesResolver(),
    ExportType.FAILED_LINKED_BIB_UPDATES, failedLinkedBibUpdatesResolver()
  );

  public String resolve(JobCommand jobCommand, String workDir, String jobId) {
    Date now = new Date();
    return Optional.ofNullable(resolvers.get(jobCommand.getExportType()))
      .map(resolver -> resolver.apply(jobCommand, workDir))
      .orElse(String.format("%s%s_%tF_%tT_%s", workDir, jobCommand.getExportType(), now, now, jobId));
  }

  private BiFunction<JobCommand, String, String> eHoldingsResolver() {
    return (jobCommand, workDir) -> {
      try {
        var exportConfigStr = jobCommand.getJobParameters().getString("eHoldingsExportConfig");
        var config = objectMapper.readValue(exportConfigStr, EHoldingsExportConfig.class);
        var recordId = config.getRecordId();
        String fileSuffix;
        if (config.getRecordType() == EHoldingsExportConfig.RecordTypeEnum.RESOURCE) {
          fileSuffix = String.format("%s_resource.csv", recordId);
        } else {
          fileSuffix = String.format("%s_package.csv", recordId);
        }
        return String.format(NAME_FORMAT, workDir, dateFormat.format(LocalDateTime.now()), fileSuffix);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(e);
      }
    };
  }

  private BiFunction<JobCommand, String, String> authHeadingsUpdatesResolver() {
    return (jobCommand, workDir) ->
      String.format(NAME_FORMAT, workDir, dateFormat.format(LocalDateTime.now()), "auth_headings_updates.csv");
  }

  private BiFunction<JobCommand, String, String> failedLinkedBibUpdatesResolver() {
    return (jobCommand, workDir) ->
      String.format(NAME_FORMAT, workDir, dateFormat.format(LocalDateTime.now()), "failed_linked_bib_updates.csv");
  }

  private BiFunction<JobCommand, String, String> bulkEditResolver() {
    return (jobCommand, workDir) -> workDir + jobCommand.getId() + "/" + LocalDate.now() + MATCHED_RECORDS + "query";
  }
}
