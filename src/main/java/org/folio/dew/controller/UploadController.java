package org.folio.dew.controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.JobCommand;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.service.JobCommandsReceiverService;
import org.openapitools.api.BulkEditApi;
import org.springframework.batch.core.Job;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static java.lang.String.format;

@RestController
@RequestMapping("/data-export-worker")
@Log4j2
@RequiredArgsConstructor
public class UploadController implements BulkEditApi {

  private static final String FILE_STORAGE_PATH = "./storage/files/";
  private static final String FILE_NAME_PATTERN = "bulk-edit-%s.csv";
  private static final String FILE_UPLOAD_ERROR = "Cannot upload a file. Reason: %s.";
  private static final String JOB_COMMAND_NOT_FOUND_ERROR = "JobCommand with id %s doesn't exist.";

  private final JobCommandsReceiverService jobCommandsReceiverService;
  private final ExportJobManager exportJobManager;
  private final List<Job> jobs;

  @Override
  public ResponseEntity<String> uploadCsvFile(UUID jobId, String body) {

    try {

      if (StringUtils.isEmpty(body)) {
        return new ResponseEntity<>(format(FILE_UPLOAD_ERROR, "file is empty"), HttpStatus.BAD_REQUEST);
      }

      if (!Files.exists(Paths.get(FILE_STORAGE_PATH))) {
        log.debug("Directory for file storing doesn't exist. Creating a directory.");
        Files.createDirectories(Paths.get(FILE_STORAGE_PATH));
      }

      Optional<JobCommand> optionalJobCommand = jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString());
      if (optionalJobCommand.isEmpty()) {
        String msg = format(JOB_COMMAND_NOT_FOUND_ERROR, jobId);
        log.debug(msg);
        return new ResponseEntity<>(msg, HttpStatus.NOT_FOUND);
      }

      String fileName = format(FILE_NAME_PATTERN, jobId);
      Path path = Files.createFile(Paths.get(FILE_STORAGE_PATH + fileName));
      Files.write(path, body.getBytes(StandardCharsets.UTF_8));
      log.info("File has been uploaded successfully.");

      var jobLaunchRequest =
        new JobLaunchRequest(
          getBulkEditJob(),
          optionalJobCommand.get().getJobParameters());

      log.info("Launching bulk edit job.");
      exportJobManager.launchJob(jobLaunchRequest);

      return new ResponseEntity<>(HttpStatus.OK);

    } catch (Exception e) {
      String errorMessage = format(FILE_UPLOAD_ERROR, e.getMessage());
      log.error(errorMessage);
      return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }

  }

  private Job getBulkEditJob() {
    return jobs.stream().filter(job -> job.getName().equals("BULK-EDIT")).findFirst().get();
  }

}
