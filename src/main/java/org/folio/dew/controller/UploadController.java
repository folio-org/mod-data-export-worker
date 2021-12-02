package org.folio.dew.controller;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Optional.ofNullable;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.des.domain.dto.JobCommand;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.service.JobCommandsReceiverService;
import org.openapitools.api.BulkEditApi;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/data-export-worker")
@Log4j2
@RequiredArgsConstructor
public class UploadController implements BulkEditApi {
  private static final String FILE_STORAGE_PATH = "./storage/files/";
  private static final String OUTPUT_FILE_NAME_PATTERN = "%s-Matched-Records-%s";
  private static final String FILE_UPLOAD_ERROR = "Cannot upload a file. Reason: %s.";
  private static final String JOB_COMMAND_NOT_FOUND_ERROR = "JobCommand with id %s doesn't exist.";

  private final JobCommandsReceiverService jobCommandsReceiverService;
  private final ExportJobManager exportJobManager;
  private final List<Job> jobs;

  @Override
  public ResponseEntity<String> uploadCsvFile(UUID jobId, MultipartFile fileName) {
    if (fileName.isEmpty()) {
      return new ResponseEntity<>(format(FILE_UPLOAD_ERROR, "file is empty"), HttpStatus.BAD_REQUEST);
    }

    Optional<JobCommand> optionalJobCommand = jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString());
    if (optionalJobCommand.isEmpty()) {
      String msg = format(JOB_COMMAND_NOT_FOUND_ERROR, jobId);
      log.debug(msg);
      return new ResponseEntity<>(msg, HttpStatus.NOT_FOUND);
    }

    var identifiersFileName = FILE_STORAGE_PATH + fileName.getOriginalFilename();

    try {
      var identifiersFilePath = Files.createFile(Paths.get(identifiersFileName));
      Files.write(identifiersFilePath, fileName.getBytes());
      log.info("File {} has been uploaded successfully.", fileName.getOriginalFilename());

      var parameters = optionalJobCommand.get().getJobParameters().getParameters();
      parameters.put("identifiersFileName", new JobParameter(identifiersFileName));
      parameters.put("outputCsvFileName", new JobParameter(FILE_STORAGE_PATH+ format(OUTPUT_FILE_NAME_PATTERN, LocalDate.now().format(ofPattern("yyyy-MM-dd")), identifiersFileName)));
      ofNullable(optionalJobCommand.get().getIdentifierType()).ifPresent(type ->
        parameters.put("identifierType", new JobParameter(type.getValue())));
      ofNullable(optionalJobCommand.get().getEntityType()).ifPresent(type ->
        parameters.put("entityType", new JobParameter(type.getValue())));

      var jobLaunchRequest =
        new JobLaunchRequest(
          getBulkEditJob(),
          optionalJobCommand.get().getJobParameters());

      log.info("Launching bulk edit job.");
      exportJobManager.launchJob(jobLaunchRequest);
    } catch (Exception e) {
      String errorMessage = format(FILE_UPLOAD_ERROR, e.getMessage());
      log.error(errorMessage);
      return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }

  private Job getBulkEditJob() {
    return jobs.stream().filter(job -> job.getName().equals("BULK-EDIT-IDENTIFIER")).findFirst().get();
  }

}
