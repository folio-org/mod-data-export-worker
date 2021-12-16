package org.folio.dew.controller;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_QUERY;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.ExportType;
import org.folio.de.entity.JobCommand;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.service.JobCommandsReceiverService;
import org.openapitools.api.JobIdApi;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/bulk-edit")
@Log4j2
@RequiredArgsConstructor
public class UploadController implements JobIdApi {
  private static final String TMP_DIR_PROPERTY = "java.io.tmpdir";
  private static final String PATH_SEPARATOR = "/";
  private static final String OUTPUT_FILE_NAME_PATTERN = "%s-Matched-Records-%s";
  private static final String FILE_UPLOAD_ERROR = "Cannot upload a file. Reason: %s.";
  private static final String JOB_COMMAND_NOT_FOUND_ERROR = "JobCommand with id %s doesn't exist.";
  public static final String IDENTIFIERS_FILE_NAME = "identifiersFileName";

  private final UserClient userClient;
  private final JobCommandsReceiverService jobCommandsReceiverService;
  private final ExportJobManager exportJobManager;
  private final List<Job> jobs;

  @Value("${spring.application.name}")
  private String springApplicationName;
  private String workDir;
  private String identifiersFileName;

  @PostConstruct
  public void postConstruct() {
    workDir = System.getProperty(TMP_DIR_PROPERTY) + PATH_SEPARATOR + springApplicationName + PATH_SEPARATOR;
  }

  @Override
  public ResponseEntity<Object> getPreviewByJobId(@ApiParam(value = "UUID of the JobCommand",required=true) @PathVariable("jobId") UUID jobId,@NotNull @ApiParam(value = "The numbers of items to return", required = true) @Valid @RequestParam(value = "limit") Integer limit) {
    var optionalJobCommand = jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString());

    if (optionalJobCommand.isEmpty()) {
      String msg = format(JOB_COMMAND_NOT_FOUND_ERROR, jobId);
      log.debug(msg);
      return new ResponseEntity<>(msg, HttpStatus.NOT_FOUND);
    }

    var jobCommand = optionalJobCommand.get();

    var fileName = extractQueryFromJobCommand(jobCommand, IDENTIFIERS_FILE_NAME);
    var exportType = jobCommand.getExportType();
    try {
      if (BULK_EDIT_IDENTIFIERS == exportType) {
        return new ResponseEntity<>(userClient.getUserByQuery(buildBarcodesQuery(fileName, limit), limit), HttpStatus.OK);
      } else if (BULK_EDIT_QUERY == exportType) {
        return new ResponseEntity<>(userClient.getUserByQuery(extractQueryFromJobCommand(jobCommand, "query"), limit), HttpStatus.OK);
      } else {
        log.error(format("Non-supported export type: %s of the jobId=%s", exportType.getValue(), jobId));
        return new ResponseEntity<>(format("Non-supported export type: %s", exportType.getValue()), HttpStatus.BAD_REQUEST);
      }
    } catch(Exception e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private String extractQueryFromJobCommand(JobCommand jobCommand, String parameterName) {
    return BULK_EDIT_IDENTIFIERS.equals(jobCommand.getExportType())
      ? (String) jobCommand.getJobParameters().getParameters().get(parameterName).getValue()
      : jobCommand.getJobParameters().getString(parameterName);
  }

  @Override
  public ResponseEntity<String> uploadCsvFile(UUID jobId, MultipartFile file) {
    if (file.isEmpty()) {
      return new ResponseEntity<>(format(FILE_UPLOAD_ERROR, "file is empty"), HttpStatus.BAD_REQUEST);
    }

    Optional<JobCommand> optionalJobCommand = jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString());
    if (optionalJobCommand.isEmpty()) {
      String msg = format(JOB_COMMAND_NOT_FOUND_ERROR, jobId);
      log.debug(msg);
      return new ResponseEntity<>(msg, HttpStatus.NOT_FOUND);
    }
    var jobCommand = optionalJobCommand.get();

    identifiersFileName = workDir + file.getOriginalFilename();

    try {
      Files.deleteIfExists(Paths.get(identifiersFileName));
      var identifiersFilePath = Files.createFile(Paths.get(identifiersFileName));
      Files.write(identifiersFilePath, file.getBytes());
      log.info("File {} has been uploaded successfully.", file.getOriginalFilename());

      prepareJobParameters(jobCommand);

      var jobLaunchRequest =
        new JobLaunchRequest(
          getBulkEditJob(jobCommand.getExportType()),
          jobCommand.getJobParameters());

      log.info("Launching bulk edit job.");
      exportJobManager.launchJob(jobLaunchRequest);
    } catch (Exception e) {
      String errorMessage = format(FILE_UPLOAD_ERROR, e.getMessage());
      log.error(errorMessage);
      return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }

  private Job getBulkEditJob(ExportType exportType) {
    return jobs.stream()
      .filter(job -> exportType.getValue().equals(job.getName()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Job was not found, aborting"));
  }

  private void prepareJobParameters(JobCommand jobCommand) throws IOException {
    var parameters = jobCommand.getJobParameters().getParameters();
    parameters.put(IDENTIFIERS_FILE_NAME, new JobParameter(identifiersFileName));
    parameters.put(TEMP_OUTPUT_FILE_PATH, new JobParameter(workDir + format(OUTPUT_FILE_NAME_PATTERN, LocalDate.now().format(ofPattern("yyyy-MM-dd")), FilenameUtils.getBaseName(identifiersFileName))));
    ofNullable(jobCommand.getIdentifierType()).ifPresent(type ->
      parameters.put("identifierType", new JobParameter(type.getValue())));
    ofNullable(jobCommand.getEntityType()).ifPresent(type ->
      parameters.put("entityType", new JobParameter(type.getValue())));
    jobCommand.setJobParameters(new JobParameters(parameters));
  }

  public String readQueryFromFile(String fileName) {
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
      var res =  bufferedReader.readLine();
      return res;
    } catch (Exception e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  private String buildBarcodesQuery(String fileName, int limit) throws IOException {
    return String.format("barcode==(%s)", Files.lines(Paths.get(fileName))
      .limit(limit)
      .map(String::strip)
      .map(i -> i.replace("\"", ""))
      .collect(joining(" OR ")));
  }
}
