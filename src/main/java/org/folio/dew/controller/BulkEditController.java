package org.folio.dew.controller;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.EntityType.ITEM;
import static org.folio.dew.domain.dto.EntityType.USER;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_QUERY;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.utils.Constants.EXPORT_TYPE;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.MATCHED_RECORDS;
import static org.folio.dew.utils.Constants.TMP_DIR_PROPERTY;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.ContentUpdateCollection;
import org.folio.dew.domain.dto.Errors;
import org.folio.dew.domain.dto.ItemCollection;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.JobCommandNotFoundException;
import org.folio.dew.error.NonSupportedEntityTypeException;
import org.folio.dew.service.BulkEditItemContentUpdateService;
import org.folio.dew.service.BulkEditParseService;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.BulkEditRollBackService;
import org.folio.dew.service.JobCommandsReceiverService;
import org.folio.dew.utils.BulkEditProcessorHelper;
import org.openapitools.api.JobIdApi;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/bulk-edit")
@Log4j2
@RequiredArgsConstructor
public class BulkEditController implements JobIdApi {

  private static final String FILE_UPLOAD_ERROR = "Cannot upload a file. Reason: %s.";
  private static final String JOB_COMMAND_NOT_FOUND_ERROR = "JobCommand with id %s doesn't exist.";

  private final UserClient userClient;
  private final InventoryClient inventoryClient;
  private final JobCommandsReceiverService jobCommandsReceiverService;
  private final ExportJobManager exportJobManager;
  private final BulkEditRollBackService bulkEditRollBackService;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  private final List<Job> jobs;
  private final BulkEditItemContentUpdateService itemContentUpdateService;
  private final BulkEditParseService bulkEditParseService;

  @Value("${spring.application.name}")
  private String springApplicationName;
  private String workDir;

  @PostConstruct
  public void postConstruct() {
    workDir = System.getProperty(TMP_DIR_PROPERTY) + PATH_SEPARATOR + springApplicationName + PATH_SEPARATOR;
  }

  @Override
  public ResponseEntity<ItemCollection> postContentUpdates(@ApiParam(value = "UUID of the JobCommand",required=true) @PathVariable("jobId") UUID jobId,@ApiParam(value = "" ,required=true )  @Valid @RequestBody ContentUpdateCollection contentUpdateCollection,@ApiParam(value = "The numbers of records to return") @Valid @RequestParam(value = "limit", required = false) Integer limit) {
    if (ITEM == contentUpdateCollection.getEntityType()) {
      var itemFormats = itemContentUpdateService.processContentUpdates(getJobCommandById(jobId.toString()), contentUpdateCollection);
      return new ResponseEntity<>(prepareItemContentUpdateResponse(itemFormats, limit), HttpStatus.OK);
    }
    throw new NonSupportedEntityTypeException(format("Non-supported entity type: %s", contentUpdateCollection.getEntityType()));
  }

  @Override
  public ResponseEntity<Object> getPreviewUsersByJobId(@ApiParam(value = "UUID of the JobCommand", required = true) @PathVariable("jobId") UUID jobId, @NotNull @ApiParam(value = "The numbers of items to return", required = true) @Valid @RequestParam(value = "limit") Integer limit) {
    var jobCommand = getJobCommandById(jobId.toString());
    var fileName = extractQueryFromJobCommand(jobCommand, FILE_NAME);
    var exportType = jobCommand.getExportType();
    try {
      if (BULK_EDIT_UPDATE == exportType) {
        return new ResponseEntity<>(userClient.getUserByQuery(buildBarcodesQueryFromUpdatedRecordsFile(fileName, limit), limit), HttpStatus.OK);
      } else if (BULK_EDIT_IDENTIFIERS == exportType) {
        return USER == jobCommand.getEntityType() ?
          new ResponseEntity<>(userClient.getUserByQuery(buildQueryFromIdentifiersFile(jobCommand.getIdentifierType().getValue(), fileName, limit), limit), HttpStatus.OK) :
          new ResponseEntity<>(inventoryClient.getItemByQuery(buildQueryFromIdentifiersFile(jobCommand.getIdentifierType().getValue(), fileName, limit), limit), HttpStatus.OK);
      } else if (BULK_EDIT_QUERY == exportType) {
        return new ResponseEntity<>(userClient.getUserByQuery(extractQueryFromJobCommand(jobCommand, "query"), limit), HttpStatus.OK);
      } else {
        log.error(format("Non-supported export type: %s of the jobId=%s", exportType.getValue(), jobId));
        return new ResponseEntity<>(format("Non-supported export type: %s", exportType.getValue()), HttpStatus.BAD_REQUEST);
      }
    } catch (Exception e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public ResponseEntity<Errors> getErrorsPreviewByJobId(@ApiParam(value = "UUID of the JobCommand", required = true) @PathVariable("jobId") UUID jobId, @NotNull @ApiParam(value = "The numbers of users to return", required = true) @Valid @RequestParam(value = "limit") Integer limit) {
    var jobCommand = getJobCommandById(jobId.toString());
    var fileName = FilenameUtils.getName(jobCommand.getJobParameters().getString(FILE_NAME));

    var errors = bulkEditProcessingErrorsService.readErrorsFromCSV(jobId.toString(), fileName, limit);
    return new ResponseEntity<>(errors, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<String> uploadCsvFile(UUID jobId, MultipartFile file) {
    if (file.isEmpty()) {
      return new ResponseEntity<>(format(FILE_UPLOAD_ERROR, "file is empty"), HttpStatus.BAD_REQUEST);
    }

    var jobCommand = getJobCommandById(jobId.toString());
    var uploadedPath = Path.of(workDir, file.getOriginalFilename());

    try {
      if (Files.exists(uploadedPath)) {
        FileUtils.forceDelete(uploadedPath.toFile());
      }
      Files.write(uploadedPath, file.getBytes());
      log.info("File {} has been uploaded successfully.", file.getOriginalFilename());
      prepareJobParameters(jobCommand, uploadedPath.toString());
      if (!isBulkEditUpdate(jobCommand) && ITEM != jobCommand.getEntityType()) {
        var job = getBulkEditJob(jobCommand);
        var jobLaunchRequest = new JobLaunchRequest(job, jobCommand.getJobParameters());
        log.info("Launching bulk edit user identifiers job.");
        exportJobManager.launchJob(jobLaunchRequest);
      }
      return new ResponseEntity<>(Long.toString(countLines(uploadedPath, isBulkEditUpdate(jobCommand))), HttpStatus.OK);
    } catch (Exception e) {
      String errorMessage = format(FILE_UPLOAD_ERROR, e.getMessage());
      log.error(errorMessage);
      return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public ResponseEntity<String> rollBackCsvFile(UUID jobId) {
    var message = bulkEditRollBackService.stopAndRollBackJobExecutionByJobId(jobId);
    return new ResponseEntity<>(message, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<String> startJob(UUID jobId) {
    var jobCommand = getJobCommandById(jobId.toString());
    var job =  getBulkEditJob(jobCommand);
    var jobLaunchRequest = new JobLaunchRequest(job, jobCommand.getJobParameters());
    try {
      log.info("Launching bulk-edit job.");
      var execution = exportJobManager.launchJob(jobLaunchRequest);
      if (isBulkEditUpdate(jobCommand)) {
        bulkEditRollBackService.putExecutionInfoPerJob(execution.getId(), jobId);
      }
    } catch (Exception e) {
      var errorMessage = e.getMessage();
      log.error(errorMessage);
      return new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }

  private String extractQueryFromJobCommand(JobCommand jobCommand, String parameterName) {
    return BULK_EDIT_IDENTIFIERS.equals(jobCommand.getExportType())
      ? (String) jobCommand.getJobParameters().getParameters().get(parameterName).getValue()
      : jobCommand.getJobParameters().getString(parameterName);
  }

  private Job getBulkEditJob(JobCommand jobCommand) {
    var jobName = BULK_EDIT_IDENTIFIERS == jobCommand.getExportType() || BULK_EDIT_UPDATE == jobCommand.getExportType() ?
      jobCommand.getExportType().getValue() + "-" + jobCommand.getEntityType() :
      jobCommand.getExportType().getValue();
    return jobs.stream()
      .filter(job -> job.getName().contains(jobName))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Job was not found, aborting"));
  }

  private void prepareJobParameters(JobCommand jobCommand, String fileName) {
    var paramsBuilder = new JobParametersBuilder(jobCommand.getJobParameters());
    paramsBuilder.addString(FILE_NAME, fileName);
    paramsBuilder.addString(TEMP_OUTPUT_FILE_PATH, workDir + LocalDate.now() + MATCHED_RECORDS + FilenameUtils.getBaseName(fileName));
    paramsBuilder.addString(EXPORT_TYPE, jobCommand.getExportType().getValue());
    ofNullable(jobCommand.getIdentifierType()).ifPresent(type ->
      paramsBuilder.addString("identifierType", type.getValue()));
    ofNullable(jobCommand.getEntityType()).ifPresent(type ->
      paramsBuilder.addString("entityType", type.getValue()));
    jobCommand.setJobParameters(paramsBuilder.toJobParameters());
  }

  private String buildQueryFromIdentifiersFile(String identifierName, String fileName, int limit) throws IOException {
    var identifiers = "";
    try (var lines = Files.lines(Paths.get(fileName))) {
      identifiers = lines.limit(limit)
        .map(String::strip)
        .map(i -> i.replace("\"", ""))
        .collect(joining(" OR "));
    }
    return String.format("%s==(%s)", BulkEditProcessorHelper.resolveIdentifier(identifierName), identifiers);
  }

  private String buildBarcodesQueryFromUpdatedRecordsFile(String fileName, int limit) throws IOException {
    var barcodes = "";
    try (var lines = Files.lines(Paths.get(fileName))) {
      barcodes = lines
        .skip(1) // skip first line with headers
        .limit(limit)
        .map(this::extractBarcodeFromUserCsvLine)
        .collect(Collectors.joining(" OR "));
    }
    return String.format("barcode==(%s)", barcodes);
  }

  private String extractBarcodeFromUserCsvLine(String csvLine) {
    var tokens = csvLine.split(",");
    var barcodeIndex = Arrays.asList(UserFormat.getUserFieldsArray()).indexOf("barcode");
    return (tokens.length > barcodeIndex + 1) ? tokens[barcodeIndex] : EMPTY;
  }

  private long countLines(Path path, boolean skipHeaders) throws IOException {
    try (var lines = Files.lines(path)) {
      return skipHeaders ? lines.count() - 1 : lines.count();
    }
  }

  private boolean isBulkEditUpdate(JobCommand jobCommand) {
    return jobCommand.getExportType() == BULK_EDIT_UPDATE;
  }

  private JobCommand getJobCommandById(String jobId) {
    var jobCommandOptional = jobCommandsReceiverService.getBulkEditJobCommandById(jobId);
    if (jobCommandOptional.isEmpty()) {
      String msg = format(JOB_COMMAND_NOT_FOUND_ERROR, jobId);
      log.debug(msg);
      throw new JobCommandNotFoundException(msg);
    }
    return jobCommandOptional.get();
  }

  private ItemCollection prepareItemContentUpdateResponse(List<ItemFormat> itemFormats, Integer limit) {
      var items = itemFormats.stream()
        .limit(isNull(limit) ? Integer.MAX_VALUE : limit)
        .map(bulkEditParseService::mapItemFormatToItem)
        .collect(Collectors.toList());
      return new ItemCollection().items(items).totalRecords(items.size());
  }
}
