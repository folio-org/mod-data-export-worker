package org.folio.dew.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.Error;
import org.folio.des.domain.dto.Errors;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.openapitools.api.BulkEditApi;
import org.springframework.batch.core.Job;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/data-export-worker")
@Log4j2
@RequiredArgsConstructor
public class UploadController implements BulkEditApi {

  private static final String FILE_STORAGE_PATH = "./storage/files/";
  private static final String FILE_NAME_PATTERN = "bulk-edit-%s.csv";
  private static final String FILE_UPLOAD_ERROR = "Cannot upload a file. Reason: %s.";

  private final ExportJobManager exportJobManager;
  private final IAcknowledgementRepository acknowledgementRepository;
  private final List<Job> jobs;

  @Override
  public ResponseEntity<String> uploadUuidCsvFile(UUID jobId, String body) {

    try {

      if (StringUtils.isEmpty(body)) {
        return new ResponseEntity<>(String.format(FILE_UPLOAD_ERROR, "file is empty"), HttpStatus.BAD_REQUEST);
      }

      if (!Files.exists(Paths.get(FILE_STORAGE_PATH))) {
        log.debug("Directory for file storing doesn't exist. Creating a directory.");
        Files.createDirectories(Paths.get(FILE_STORAGE_PATH));
      }

      String fileName = String.format(FILE_NAME_PATTERN, jobId.toString());
      Path path = Files.createFile(Paths.get(FILE_STORAGE_PATH + fileName));
      Files.write(path, body.getBytes(StandardCharsets.UTF_8));
      log.info("File has been uploaded successfully.");

//      var jobLaunchRequest =
//        new JobLaunchRequest(
//          jobMap.get(jobCommand.getExportType().toString()),
//          jobCommand.getJobParameters());
//
//      acknowledgementRepository.addAcknowledgement(jobCommand.getId().toString(), acknowledgment);
//      exportJobManager.launchJob(jobLaunchRequest);
      
      return new ResponseEntity<>(HttpStatus.OK);
      
    } catch (Exception e) {
      String errorMessage = String.format(FILE_UPLOAD_ERROR, e.getMessage()); 
      log.error(errorMessage);
      return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    BulkEditApi.super.uploadUuidCsvFile(jobId, body);
  }
}
