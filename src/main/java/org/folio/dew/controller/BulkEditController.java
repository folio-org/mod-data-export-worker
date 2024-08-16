package org.folio.dew.controller;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_MARC_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_MARC_PATH;
import static org.folio.dew.utils.Constants.BULKEDIT_DIR_NAME;
import static org.folio.dew.utils.Constants.EXPORT_TYPE;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.FILE_UPLOAD_ERROR;
import static org.folio.dew.utils.Constants.MARC_RECORDS;
import static org.folio.dew.utils.Constants.MATCHED_RECORDS;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.TEMP_IDENTIFIERS_FILE_NAME;
import static org.folio.dew.utils.Constants.TOTAL_CSV_LINES;
import static org.folio.dew.utils.Constants.getWorkingDirectory;
import static org.folio.dew.utils.CsvHelper.countLines;
import static org.folio.dew.utils.SystemHelper.getTempDirWithSeparatorSuffix;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import io.swagger.annotations.ApiParam;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.batch.ExportJobManagerSync;
import org.folio.dew.domain.dto.Errors;
import org.folio.dew.error.NotFoundException;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.JobCommandsReceiverService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.openapitools.api.JobIdApi;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/bulk-edit")
@Log4j2
@RequiredArgsConstructor
public class BulkEditController implements JobIdApi {


  private static final String JOB_COMMAND_NOT_FOUND_ERROR = "JobCommand with id %s doesn't exist.";
  private static final boolean JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE = false;

  private final JobCommandsReceiverService jobCommandsReceiverService;
  private final ExportJobManagerSync exportJobManagerSync;
  private final List<Job> jobs;
  private final LocalFilesStorage localFilesStorage;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  @Value("${spring.application.name}")
  private String springApplicationName;
  private String workDir;

  @PostConstruct
  public void postConstruct() {
    workDir = getWorkingDirectory(springApplicationName, BULKEDIT_DIR_NAME);
  }

  @Override
  public ResponseEntity<String> uploadCsvFile(UUID jobId, MultipartFile file) {
    log.info("uploadCsvFile:: for jobId={} ", jobId);
    if (file.isEmpty()) {
      log.info("uploadCsvFile:: file provided is empty for job id {}", jobId);
      return new ResponseEntity<>(format(FILE_UPLOAD_ERROR, "file is empty"), HttpStatus.BAD_REQUEST);
    }

    var jobCommand = getJobCommandById(jobId.toString());
    var uploadedPath = workDir + jobId + PATH_SEPARATOR + file.getOriginalFilename();

    try {
      localFilesStorage.delete(uploadedPath);
      localFilesStorage.write(uploadedPath, file.getBytes());
      String tempIdentifiersFile = null;
      log.info("uploadCsvFile:: file is uploaded for jobId={} with jobExportType={}", jobId, jobCommand.getExportType());
      if (BULK_EDIT_IDENTIFIERS.equals(jobCommand.getExportType())) {
        tempIdentifiersFile = saveTemporaryIdentifiersFile(jobId, file);
      }
      prepareJobParameters(jobCommand, uploadedPath, tempIdentifiersFile);
      jobCommandsReceiverService.updateJobCommand(jobCommand);
      log.info("File {} has been uploaded successfully.", file.getOriginalFilename());
      var job = getBulkEditJob(jobCommand);
      var jobLaunchRequest = new JobLaunchRequest(job, jobCommand.getJobParameters());
      log.info("Launching bulk edit identifiers job.");
      new Thread(getRunnableWithCurrentFolioContext(() -> {
        try {
          exportJobManagerSync.launchJob(jobLaunchRequest);
        } catch (JobExecutionException e) {
          String errorMessage = format(FILE_UPLOAD_ERROR, e.getMessage());
          log.error(errorMessage);
        }
      })).start();
      var numberOfLines = jobCommand.getJobParameters().getLong(TOTAL_CSV_LINES);
      return new ResponseEntity<>(Long.toString(isNull(numberOfLines) ? 0 : numberOfLines), HttpStatus.OK);
    } catch (Exception e) {
      String errorMessage = format(FILE_UPLOAD_ERROR, e.getMessage());
      log.error(errorMessage);
      return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private String saveTemporaryIdentifiersFile(UUID jobId, MultipartFile file) throws IOException {
    var tempDir = getTempDirWithSeparatorSuffix() + springApplicationName + PATH_SEPARATOR + jobId;
    var tempFilePath = tempDir + PATH_SEPARATOR + file.getOriginalFilename();
    var path = Path.of(tempFilePath);
    Files.deleteIfExists(path);
    Files.createDirectories(Path.of(tempDir));
    Files.write(path, file.getBytes());
    log.info("Saved temporary identifiers file: {}", tempFilePath);
    return tempFilePath;
  }

  @Override
  public ResponseEntity<String> startJob(UUID jobId) {
    var jobCommand = getJobCommandById(jobId.toString());
    var job =  getBulkEditJob(jobCommand);
    var jobLaunchRequest = new JobLaunchRequest(job, jobCommand.getJobParameters());
    try {
      log.info("Launching bulk-edit job.");
      new Thread(getRunnableWithCurrentFolioContext(() -> {
        try {
          exportJobManagerSync.launchJob(jobLaunchRequest);
        } catch (JobExecutionException e) {
          log.error(e.getMessage());
        }
      })).start();
    } catch (Exception e) {
      var errorMessage = e.getMessage();
      log.error(errorMessage);
      return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Errors> getErrorsPreviewByJobId(@ApiParam(value = "UUID of the JobCommand", required = true) @PathVariable("jobId") UUID jobId, @NotNull @ApiParam(value = "The numbers of users to return", required = true) @Valid @RequestParam(value = "limit") Integer limit) {
    var jobCommand = getJobCommandById(jobId.toString());
    var fileName = jobCommand.getId() + PATH_SEPARATOR + FilenameUtils.getName(jobCommand.getJobParameters().getString(FILE_NAME));
    log.info("downloadHoldingsPreviewByJobId:: fileName={}", fileName);

    var errors = bulkEditProcessingErrorsService.readErrorsFromCSV(jobId.toString(), fileName, limit);
    return new ResponseEntity<>(errors, HttpStatus.OK);
  }

  private Job getBulkEditJob(JobCommand jobCommand) {
    var jobName = BULK_EDIT_IDENTIFIERS == jobCommand.getExportType() ?
      jobCommand.getExportType().getValue() + "-" + jobCommand.getEntityType() :
      jobCommand.getExportType().getValue();
    return jobs.stream()
      .filter(job -> job.getName().contains(jobName))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Job was not found, aborting"));
  }

  private void prepareJobParameters(JobCommand jobCommand, String uploadedPath, String tempIdentifiersFile) throws IOException {
    var paramsBuilder = new JobParametersBuilder(jobCommand.getJobParameters());
    ofNullable(tempIdentifiersFile).ifPresent(path -> paramsBuilder.addString(TEMP_IDENTIFIERS_FILE_NAME, path, JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE));
    paramsBuilder.addString(FILE_NAME, uploadedPath, JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE);
    paramsBuilder.addLong(TOTAL_CSV_LINES, countLines(localFilesStorage, uploadedPath), JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE);
    var fileName = jobCommand.getId() + PATH_SEPARATOR + LocalDate.now() + MATCHED_RECORDS + FilenameUtils.getBaseName(uploadedPath);
    paramsBuilder.addString(TEMP_OUTPUT_FILE_PATH, workDir + fileName, JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE);
    paramsBuilder.addString(TEMP_LOCAL_FILE_PATH, getTempDirWithSeparatorSuffix() + springApplicationName + PATH_SEPARATOR + fileName, JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE);
    paramsBuilder.addString(EXPORT_TYPE, jobCommand.getExportType().getValue(), JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE);
    var marcFileName = jobCommand.getId() + PATH_SEPARATOR + LocalDate.now() + MARC_RECORDS + FilenameUtils.getBaseName(uploadedPath);
    paramsBuilder.addString(TEMP_OUTPUT_MARC_PATH, workDir + marcFileName);
    paramsBuilder.addString(TEMP_LOCAL_MARC_PATH, getTempDirWithSeparatorSuffix() + springApplicationName + PATH_SEPARATOR + marcFileName, JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE);
    ofNullable(jobCommand.getIdentifierType()).ifPresent(type ->
      paramsBuilder.addString("identifierType", type.getValue(), JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE));
    ofNullable(jobCommand.getEntityType()).ifPresent(type ->
      paramsBuilder.addString("entityType", type.getValue(), JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE));
    jobCommand.setJobParameters(paramsBuilder.toJobParameters());
  }

  private JobCommand getJobCommandById(String jobId) {
    var jobCommandOptional = jobCommandsReceiverService.getBulkEditJobCommandById(jobId);
    if (jobCommandOptional.isEmpty()) {
      String msg = format(JOB_COMMAND_NOT_FOUND_ERROR, jobId);
      log.error(msg);
      throw new NotFoundException(msg);
    }
    return jobCommandOptional.get();
  }

}
