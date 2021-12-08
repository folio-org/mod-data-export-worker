package org.folio.dew.controller;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Optional.ofNullable;
import static org.folio.des.domain.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.des.domain.dto.ExportType.BULK_EDIT_QUERY;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.JobCommand;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;

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

  private void prepareJobParameters(JobCommand jobCommand) {
    var parameters = jobCommand.getJobParameters().getParameters();
    parameters.put("identifiersFileName", new JobParameter(identifiersFileName));
    parameters.put(TEMP_OUTPUT_FILE_PATH, new JobParameter(workDir + format(OUTPUT_FILE_NAME_PATTERN, LocalDate.now().format(ofPattern("yyyy-MM-dd")), FilenameUtils.getBaseName(identifiersFileName))));
    ofNullable(jobCommand.getIdentifierType()).ifPresent(type ->
      parameters.put("identifierType", new JobParameter(type.getValue())));
    ofNullable(jobCommand.getEntityType()).ifPresent(type ->
      parameters.put("entityType", new JobParameter(type.getValue())));
    if (BULK_EDIT_QUERY.equals(jobCommand.getExportType())) {
      parameters.put("query", new JobParameter(readQueryFromFile(identifiersFileName)));
    }
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
}
