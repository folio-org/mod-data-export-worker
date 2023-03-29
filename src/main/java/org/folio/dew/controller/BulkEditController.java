package org.folio.dew.controller;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.dew.domain.dto.EntityType.ITEM;
import static org.folio.dew.domain.dto.EntityType.USER;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.PREVIEW_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.QUERY;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.BULKEDIT_DIR_NAME;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.EXPORT_TYPE;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.FILE_UPLOAD_ERROR;
import static org.folio.dew.utils.Constants.IDENTIFIER_TYPE;
import static org.folio.dew.utils.Constants.INITIAL_PREFIX;
import static org.folio.dew.utils.Constants.MATCHED_RECORDS;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.TEMP_IDENTIFIERS_FILE_NAME;
import static org.folio.dew.utils.Constants.TOTAL_CSV_LINES;
import static org.folio.dew.utils.Constants.PREVIEW_PREFIX;
import static org.folio.dew.utils.Constants.getWorkingDirectory;
import static org.folio.dew.utils.CsvHelper.countLines;
import static org.folio.dew.utils.SystemHelper.getTempDirWithSeparatorSuffix;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import com.opencsv.CSVReader;
import io.swagger.annotations.ApiParam;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.batch.ExportJobManagerSync;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.Errors;
import org.folio.dew.domain.dto.HoldingsContentUpdateCollection;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsRecordCollection;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.ItemCollection;
import org.folio.dew.domain.dto.ItemContentUpdateCollection;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.domain.dto.UserCollection;
import org.folio.dew.domain.dto.UserContentUpdateCollection;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.FileOperationException;
import org.folio.dew.error.NonSupportedEntityException;
import org.folio.dew.error.NotFoundException;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.dew.service.BulkEditItemContentUpdateService;
import org.folio.dew.service.BulkEditParseService;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.BulkEditRollBackService;
import org.folio.dew.service.JobCommandsReceiverService;
import org.folio.dew.service.UpdatesResult;
import org.folio.dew.service.mapper.HoldingsMapper;
import org.folio.dew.service.update.BulkEditHoldingsContentUpdateService;
import org.folio.dew.service.update.BulkEditUserContentUpdateService;
import org.folio.dew.utils.CsvHelper;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.openapitools.api.JobIdApi;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
  private static final String FAILED_TO_READ_FILE_ERROR = "Failed to read %s for job id %s, reason: %s";

  private final UserClient userClient;
  private final InventoryClient inventoryClient;
  private final HoldingClient holdingClient;
  private final JobCommandsReceiverService jobCommandsReceiverService;
  private final ExportJobManagerSync exportJobManagerSync;
  private final BulkEditRollBackService bulkEditRollBackService;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  private final List<Job> jobs;
  private final BulkEditItemContentUpdateService itemContentUpdateService;
  private final BulkEditUserContentUpdateService userContentUpdateService;
  private final BulkEditHoldingsContentUpdateService holdingsContentUpdateService;
  private final BulkEditParseService bulkEditParseService;
  private final HoldingsMapper holdingsMapper;
  private final RemoteFilesStorage remoteFilesStorage;
  private final LocalFilesStorage localFilesStorage;
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
  public ResponseEntity<ItemCollection> postItemContentUpdates(@ApiParam(value = "UUID of the JobCommand",required=true) @PathVariable("jobId") UUID jobId,@ApiParam(value = "" ,required=true )  @Valid @RequestBody ItemContentUpdateCollection contentUpdateCollection,@ApiParam(value = "The numbers of records to return") @Valid @RequestParam(value = "limit", required = false) Integer limit) {
    var jobCommand = prepareForContentUpdates(jobId);
    var updatesResult = itemContentUpdateService.processContentUpdates(jobCommand, contentUpdateCollection);
    jobCommandsReceiverService.updateJobCommand(jobCommand);
    return new ResponseEntity<>(prepareItemContentUpdateResponse(updatesResult, limit), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<UserCollection> postUserContentUpdates(@ApiParam(value = "UUID of the JobCommand",required=true) @PathVariable("jobId") UUID jobId, @ApiParam(value = "" ,required=true )  @Valid @RequestBody UserContentUpdateCollection contentUpdateCollection, @ApiParam(value = "The numbers of records to return") @Valid @RequestParam(value = "limit", required = false) Integer limit) {
    var jobCommand = prepareForContentUpdates(jobId);
    var updatesResult = userContentUpdateService.process(jobCommand, contentUpdateCollection);
    log.info("postUserContentUpdate: {} users", updatesResult.getEntitiesForPreview().size());
    jobCommandsReceiverService.updateJobCommand(jobCommand);
    return new ResponseEntity<>(prepareUserContentUpdateResponse(updatesResult, limit), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<HoldingsRecordCollection> postHoldingsContentUpdates(@ApiParam(value = "UUID of the JobCommand",required=true) @PathVariable("jobId") UUID jobId, @ApiParam(value = "" ,required=true )  @Valid @RequestBody HoldingsContentUpdateCollection contentUpdateCollection, @ApiParam(value = "The numbers of records to return") @Valid @RequestParam(value = "limit", required = false) Integer limit) {
    var jobCommand = prepareForContentUpdates(jobId);
    var updatesResult = holdingsContentUpdateService.process(jobCommand, contentUpdateCollection);
    jobCommandsReceiverService.updateJobCommand(jobCommand);
    return new ResponseEntity<>(prepareHoldingsContentUpdateResponse(updatesResult, limit), HttpStatus.OK);
  }

  private JobCommand prepareForContentUpdates(UUID jobId) {
    bulkEditProcessingErrorsService.removeTemporaryErrorStorage();
    var jobCommand = getJobCommandById(jobId.toString());
    if (nonNull(jobCommand.getIdentifierType())) {
      jobCommand.setJobParameters(new JobParametersBuilder(jobCommand.getJobParameters())
          .addString(IDENTIFIER_TYPE, jobCommand.getIdentifierType().getValue())
          .toJobParameters());
    }
    return jobCommand;
  }

  @Override
  public ResponseEntity<UserCollection> getPreviewUsersByJobId(@ApiParam(value = "UUID of the JobCommand", required = true) @PathVariable("jobId") UUID jobId, @NotNull @ApiParam(value = "The numbers of items to return", required = true) @Valid @RequestParam(value = "limit") Integer limit) {
    var jobCommand = getJobCommandById(jobId.toString());
    if (BULK_EDIT_IDENTIFIERS == jobCommand.getExportType()) {
      var fileName = jobCommand.getId() + PATH_SEPARATOR + FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION;
      try {
        var userFormats = CsvHelper.readRecordsFromRemoteFilesStorage(remoteFilesStorage, fileName, limit, UserFormat.class);
        var users = userFormats.stream()
          .map(bulkEditParseService::mapUserFormatToUser)
          .collect(Collectors.toList());
        return new ResponseEntity<>(new UserCollection().users(users).totalRecords(users.size()), HttpStatus.OK);
      } catch (Exception e) {
        var msg = String.format(FAILED_TO_READ_FILE_ERROR, fileName, jobCommand.getId(), e.getMessage());
        log.error(msg);
        return new ResponseEntity<>(new UserCollection().users(Collections.emptyList()).totalRecords(0), HttpStatus.OK);
      }
    } else {
      return new ResponseEntity<>(userClient.getUserByQuery(buildPreviewUsersQueryFromJobCommand(jobCommand, limit), limit), HttpStatus.OK);
    }
  }

  @Override public ResponseEntity<ItemCollection> getPreviewItemsByJobId(UUID jobId, Integer limit) {
    var jobCommand = getJobCommandById(jobId.toString());
    if (BULK_EDIT_IDENTIFIERS == jobCommand.getExportType()) {
      var fileName = jobCommand.getId() + PATH_SEPARATOR + FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION;
      try {
        var items = CsvHelper.readRecordsFromRemoteFilesStorage(remoteFilesStorage, fileName, limit, ItemFormat.class)
          .stream()
          .map(bulkEditParseService::mapItemFormatToItem)
          .collect(Collectors.toList());
        return new ResponseEntity<>(new ItemCollection().items(items).totalRecords(items.size()), HttpStatus.OK);
      } catch (Exception e) {
        var msg = String.format(FAILED_TO_READ_FILE_ERROR, fileName, jobCommand.getId(), e.getMessage());
        log.error(msg);
        return new ResponseEntity<>(new ItemCollection().items(Collections.emptyList()).totalRecords(0), HttpStatus.OK);
      }
    } else {
      return new ResponseEntity<>(inventoryClient.getItemByQuery(buildPreviewQueryFromJobCommand(jobCommand, limit), limit), HttpStatus.OK);
    }
  }

  @Override
  public ResponseEntity<HoldingsRecordCollection> getPreviewHoldingsByJobId(UUID jobId, Integer limit) {
    var jobCommand = getJobCommandById(jobId.toString());
    if (BULK_EDIT_IDENTIFIERS == jobCommand.getExportType()) {
      var fileName = jobCommand.getId() + PATH_SEPARATOR + FilenameUtils.getName(jobCommand.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH)) + CSV_EXTENSION;
      try {
        var holdings = CsvHelper.readRecordsFromRemoteFilesStorage(remoteFilesStorage, fileName, limit, HoldingsFormat.class)
          .stream()
          .map(holdingsMapper::mapToHoldingsRecord)
          .collect(Collectors.toList());
        return new ResponseEntity<>(new HoldingsRecordCollection().holdingsRecords(holdings).totalRecords(holdings.size()), HttpStatus.OK);
      } catch (Exception e) {
        var msg = String.format(FAILED_TO_READ_FILE_ERROR, fileName, jobCommand.getId(), e.getMessage());
        log.error(msg);
        return new ResponseEntity<>(new HoldingsRecordCollection().holdingsRecords(Collections.emptyList()).totalRecords(0), HttpStatus.OK);
      }
    } else {
      return new ResponseEntity<>(holdingClient.getHoldingsByQuery(buildPreviewQueryFromJobCommand(jobCommand, limit), limit), HttpStatus.OK);
    }
  }

  @Override
  public ResponseEntity<Resource> downloadItemsPreviewByJobId(@ApiParam(value = "UUID of the JobCommand", required = true) @PathVariable("jobId") UUID jobId) {
    var jobCommand = getJobCommandById(jobId.toString());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    try {
      var updatedFileName = FilenameUtils.getName(jobCommand.getJobParameters().getString(PREVIEW_FILE_NAME));
      var updatedFullPath = FilenameUtils.getFullPath(jobCommand.getJobParameters().getString(PREVIEW_FILE_NAME));
      var updatedFilePath = updatedFullPath + updatedFileName;
      var content = localFilesStorage.readAllBytes(updatedFilePath);
      ByteArrayResource updatedFileResource = new ByteArrayResource(content);
      headers.setContentLength(content.length);
      headers.setContentDispositionFormData(updatedFileName, updatedFileName);
      return ResponseEntity.ok().headers(headers).body(updatedFileResource);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(new DescriptiveResource(e.getMessage()));
    }
  }

  @Override
  public ResponseEntity<Resource> downloadUsersPreviewByJobId(@ApiParam(value = "UUID of the JobCommand", required = true) @PathVariable("jobId") UUID jobId) {
    return downloadPreviewByJobId(jobId);
  }

  @Override
  public ResponseEntity<Resource> downloadHoldingsPreviewByJobId(@ApiParam(value = "UUID of the JobCommand", required = true) @PathVariable("jobId") UUID jobId) {
    return downloadPreviewByJobId(jobId);
  }

  private ResponseEntity<Resource> downloadPreviewByJobId(UUID jobId) {
    var jobCommand = getJobCommandById(jobId.toString());
    var fileName = jobCommand.getJobParameters().getString(PREVIEW_FILE_NAME);
    if (nonNull(fileName)) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      try (InputStream is = remoteFilesStorage.newInputStream(fileName)) {
        var updatedUsersResource = new ByteArrayResource(is.readAllBytes());
        headers.setContentLength(updatedUsersResource.contentLength());
        headers.setContentDispositionFormData(fileName, fileName);
        return ResponseEntity.ok().headers(headers).body(updatedUsersResource);
      } catch (Exception e) {
        return ResponseEntity.internalServerError().body(new DescriptiveResource(e.getMessage()));
      }
    }
    throw new NotFoundException("Preview is not available");
  }

  @Override
  public ResponseEntity<Errors> getErrorsPreviewByJobId(@ApiParam(value = "UUID of the JobCommand", required = true) @PathVariable("jobId") UUID jobId, @NotNull @ApiParam(value = "The numbers of users to return", required = true) @Valid @RequestParam(value = "limit") Integer limit) {
    var jobCommand = getJobCommandById(jobId.toString());
    var fileName = jobCommand.getId() + PATH_SEPARATOR + FilenameUtils.getName(jobCommand.getJobParameters().getString(FILE_NAME));

    var errors = bulkEditProcessingErrorsService.readErrorsFromCSV(jobId.toString(), fileName, limit);
    return new ResponseEntity<>(errors, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<String> uploadCsvFile(UUID jobId, MultipartFile file) {
    if (file.isEmpty()) {
      return new ResponseEntity<>(format(FILE_UPLOAD_ERROR, "file is empty"), HttpStatus.BAD_REQUEST);
    }

    var jobCommand = getJobCommandById(jobId.toString());
    var uploadedPath = workDir + jobId + PATH_SEPARATOR + file.getOriginalFilename();

    try {
      localFilesStorage.delete(uploadedPath);
      localFilesStorage.write(uploadedPath, file.getBytes());
      String tempIdentifiersFile = null;
      if (BULK_EDIT_IDENTIFIERS.equals(jobCommand.getExportType())) {
        tempIdentifiersFile = saveTemporaryIdentifiersFile(jobId, file);
      }
      prepareJobParameters(jobCommand, uploadedPath, tempIdentifiersFile);
      jobCommandsReceiverService.updateJobCommand(jobCommand);
      if (isBulkEditUpdate(jobCommand) && jobCommand.getEntityType() == USER) {
        localFilesStorage.write(workDir + jobId + PATH_SEPARATOR + INITIAL_PREFIX + file.getOriginalFilename(), file.getBytes());
      }
      log.info("File {} has been uploaded successfully.", file.getOriginalFilename());
      if (!isBulkEditUpdate(jobCommand)) {
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
      }
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

  private Job getBulkEditJob(JobCommand jobCommand) {
    var jobName = BULK_EDIT_IDENTIFIERS == jobCommand.getExportType() || BULK_EDIT_UPDATE == jobCommand.getExportType() ?
      jobCommand.getExportType().getValue() + "-" + jobCommand.getEntityType() :
      jobCommand.getExportType().getValue();
    return jobs.stream()
      .filter(job -> job.getName().contains(jobName))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Job was not found, aborting"));
  }

  private void prepareJobParameters(JobCommand jobCommand, String uploadedPath, String tempIdentifiersFile) throws IOException {
    var paramsBuilder = new JobParametersBuilder(jobCommand.getJobParameters());
    ofNullable(tempIdentifiersFile).ifPresent(path -> paramsBuilder.addString(TEMP_IDENTIFIERS_FILE_NAME, path));
    paramsBuilder.addString(FILE_NAME, uploadedPath);
    paramsBuilder.addLong(TOTAL_CSV_LINES, countLines(localFilesStorage, uploadedPath, isBulkEditUpdate(jobCommand)));
    var fileName = jobCommand.getId() + PATH_SEPARATOR + (isBulkEditUpdate(jobCommand) ? EMPTY : LocalDate.now() + MATCHED_RECORDS) + FilenameUtils.getBaseName(uploadedPath);
    paramsBuilder.addString(TEMP_OUTPUT_FILE_PATH, workDir + fileName);
    paramsBuilder.addString(TEMP_LOCAL_FILE_PATH, getTempDirWithSeparatorSuffix() + springApplicationName + PATH_SEPARATOR + fileName);
    paramsBuilder.addString(EXPORT_TYPE, jobCommand.getExportType().getValue());
    ofNullable(jobCommand.getIdentifierType()).ifPresent(type ->
      paramsBuilder.addString("identifierType", type.getValue()));
    ofNullable(jobCommand.getEntityType()).ifPresent(type ->
      paramsBuilder.addString("entityType", type.getValue()));
    jobCommand.setJobParameters(paramsBuilder.toJobParameters());
  }

  private boolean isBulkEditUpdate(JobCommand jobCommand) {
    return jobCommand.getExportType() == BULK_EDIT_UPDATE;
  }

  private JobCommand getJobCommandById(String jobId) {
    var jobCommandOptional = jobCommandsReceiverService.getBulkEditJobCommandById(jobId);
    if (jobCommandOptional.isEmpty()) {
      String msg = format(JOB_COMMAND_NOT_FOUND_ERROR, jobId);
      log.debug(msg);
      throw new NotFoundException(msg);
    }
    return jobCommandOptional.get();
  }

  private ItemCollection prepareItemContentUpdateResponse(UpdatesResult<ItemFormat> updatesResult, Integer limit) {
      var items = updatesResult.getEntitiesForPreview().stream()
        .limit(isNull(limit) ? Integer.MAX_VALUE : limit)
        .map(bulkEditParseService::mapItemFormatToItem)
        .collect(Collectors.toList());
      return new ItemCollection().items(items).totalRecords(updatesResult.getTotal());
  }

  private UserCollection prepareUserContentUpdateResponse(UpdatesResult<UserFormat> updatesResult, Integer limit) {
    var users = updatesResult.getEntitiesForPreview().stream()
      .limit(isNull(limit) ? Integer.MAX_VALUE : limit)
      .map(bulkEditParseService::mapUserFormatToUser)
      .collect(Collectors.toList());
    return new UserCollection().users(users).totalRecords(updatesResult.getTotal());
  }

  private HoldingsRecordCollection prepareHoldingsContentUpdateResponse(UpdatesResult<HoldingsFormat> updatesResult, Integer limit) {
    var holdingsRecords = updatesResult.getEntitiesForPreview().stream()
        .limit(isNull(limit) ? Integer.MAX_VALUE : limit)
        .map(holdingsMapper::mapToHoldingsRecord)
        .collect(Collectors.toList());
    return new HoldingsRecordCollection().holdingsRecords(holdingsRecords).totalRecords(updatesResult.getTotal());
  }

  private String buildPreviewUsersQueryFromJobCommand(JobCommand jobCommand, int limit) {
    if (isBulkEditUpdate(jobCommand)) {
      ofNullable(jobCommand.getJobParameters().getString(FILE_NAME)).ifPresent(filename -> {
        var basename = FilenameUtils.getBaseName(filename);
        if (!basename.startsWith(INITIAL_PREFIX)) {
          jobCommand.setJobParameters(new JobParametersBuilder(jobCommand.getJobParameters()).addString(FILE_NAME,
            filename.replace(basename, INITIAL_PREFIX + basename)).toJobParameters());
        }
      });
    }
    return buildPreviewQueryFromJobCommand(jobCommand, limit);
  }

  private String buildPreviewQueryFromJobCommand(JobCommand jobCommand, int limit) {
    switch(jobCommand.getExportType()) {
    case BULK_EDIT_UPDATE:
      var query = buildPreviewQueryFromCsv(jobCommand, limit);
      return query.replace("()", "(default)");
    case BULK_EDIT_QUERY:
      return jobCommand.getJobParameters().getString(QUERY);
    default:
      throw new NonSupportedEntityException(format("Non-supported export type: %s", jobCommand.getExportType()));
    }
  }

  private String buildPreviewQueryFromCsv(JobCommand jobCommand, int limit) {
    var fileName = extractFileName(jobCommand);
    if (StringUtils.isEmpty(fileName)) throw new FileOperationException("File for preview is not present or was not uploaded");
    if (!fileName.contains(CSV_EXTENSION)) fileName += CSV_EXTENSION;
    try {
      Reader inputReader;
      var minioFileName = nonNull(jobCommand.getJobParameters().getString(UPDATED_FILE_NAME)) ? jobCommand.getId() + PATH_SEPARATOR + FilenameUtils.getName(fileName) : PREVIEW_PREFIX + FilenameUtils.getName(fileName);
      if (remoteFilesStorage.containsFile(minioFileName)) {
        inputReader = new InputStreamReader(remoteFilesStorage.newInputStream(minioFileName));
      } else {
        inputReader = new InputStreamReader(localFilesStorage.newInputStream(fileName));
      }
      try (var reader = new CSVReader(inputReader)) {
        var values = reader.readAll().stream()
          .skip(getNumberOfLinesToSkip(jobCommand))
          .limit(limit)
          .map(line -> extractIdentifiersFromLine(line, jobCommand))
          .map(identifier -> String.format("\"%s\"", identifier))
          .collect(Collectors.joining(" OR ", "(", ")"));
        var identifierType = getIdentifierType(jobCommand);
        return format(getMatchPattern(identifierType), resolveIdentifier(identifierType), values);
      }

    } catch (Exception e) {
      throw new FileOperationException(format("Failed to read %s file, reason: %s", fileName, e.getMessage()));
    }
  }

  private String getIdentifierType(JobCommand jobCommand) {
    if (jobCommand.getEntityType() == HOLDINGS_RECORD) {
      return IdentifierType.ID.getValue();
    }
    return  jobCommand.getIdentifierType().getValue();
  }

  private String extractIdentifiersFromLine(String[] line, JobCommand jobCommand) {
    var identifierIndex = getIdentifierIndex(jobCommand);
    if (line.length > identifierIndex + 1) {
      return line[identifierIndex];
    } else if (line.length == 1) {
      return line[0];
    }
    return EMPTY;
  }

  private int getNumberOfLinesToSkip(JobCommand jobCommand) {
    if (BULK_EDIT_UPDATE == jobCommand.getExportType()) {
      return nonNull(jobCommand.getJobParameters().getString(UPDATED_FILE_NAME)) ? 1 : 0;
    }
    return 0;
  }

  private String extractFileName(JobCommand jobCommand) {
    if (isItemUpdatePreview(jobCommand) || isHoldingUpdatePreview(jobCommand)) {
        return jobCommand.getJobParameters().getString(UPDATED_FILE_NAME);
    }
    var fileProperty = isUserUpdatePreview(jobCommand) ? TEMP_OUTPUT_FILE_PATH : FILE_NAME;
    return jobCommand.getJobParameters().getString(fileProperty);
  }

  private boolean isHoldingUpdatePreview(JobCommand jobCommand) {
    return jobCommand.getExportType() == BULK_EDIT_UPDATE && jobCommand.getEntityType() == HOLDINGS_RECORD;
  }

  private boolean isUserUpdatePreview(JobCommand jobCommand) {
    return jobCommand.getExportType() == BULK_EDIT_UPDATE && jobCommand.getEntityType() == USER;
  }

  private boolean isItemUpdatePreview(JobCommand jobCommand) {
    return jobCommand.getExportType() == BULK_EDIT_UPDATE && jobCommand.getEntityType() == ITEM;
  }

  private int getIdentifierIndex(JobCommand jobCommand) {
    if (USER == jobCommand.getEntityType()) {
      return Arrays.asList(UserFormat.getUserFieldsArray()).indexOf(resolveIdentifier(jobCommand.getIdentifierType().getValue()));
    } else if (ITEM == jobCommand.getEntityType()) {
      return Arrays.asList(ItemFormat.getItemFieldsArray()).indexOf(resolveIdentifier(jobCommand.getIdentifierType().getValue()));
    } else if (HOLDINGS_RECORD == jobCommand.getEntityType()) {
      return  Arrays.asList(HoldingsFormat.getHoldingsFieldsArray()).indexOf(IdentifierType.ID.getValue().toLowerCase());
    } else {
      throw new NonSupportedEntityException(format("Non-supported entity type: %s", jobCommand.getEntityType()));
    }
  }
}
