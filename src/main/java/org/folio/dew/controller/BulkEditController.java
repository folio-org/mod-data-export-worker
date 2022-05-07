package org.folio.dew.controller;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.EntityType.ITEM;
import static org.folio.dew.domain.dto.EntityType.USER;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.QUERY;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.EXPORT_TYPE;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.MATCHED_RECORDS;
import static org.folio.dew.utils.Constants.TMP_DIR_PROPERTY;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.PREVIEW_USERS_QUERY;
import static org.folio.dew.utils.Constants.PREVIEW_ITEMS_QUERY;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
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
import org.folio.dew.error.FileOperationException;
import org.folio.dew.error.NotFoundException;
import org.folio.dew.error.NonSupportedEntityException;
import org.folio.dew.service.BulkEditItemContentUpdateService;
import org.folio.dew.service.BulkEditParseService;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.BulkEditRollBackService;
import org.folio.dew.service.JobCommandsReceiverService;
import org.folio.dew.utils.CsvHelper;
import org.folio.spring.FolioExecutionContext;
import org.openapitools.api.JobIdApi;
import org.springframework.batch.core.Job;
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
  private final FolioExecutionContext folioExecutionContext;

  @Value("${spring.application.name}")
  private String springApplicationName;
  private String workDir;
  private final Map<UUID, UUID> lastJobIdentifiersByCurrentUser = new ConcurrentHashMap<>();

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
    throw new NonSupportedEntityException(format("Non-supported entity type: %s", contentUpdateCollection.getEntityType()));
  }

  @Override
  public ResponseEntity<Object> getPreviewUsersByJobId(@ApiParam(value = "UUID of the JobCommand", required = true) @PathVariable("jobId") UUID jobId, @NotNull @ApiParam(value = "The numbers of items to return", required = true) @Valid @RequestParam(value = "limit") Integer limit) {
    var jobCommand = getJobCommandById(jobId.toString());
    var lastJobIdentifiersId = lastJobIdentifiersByCurrentUser.get(folioExecutionContext.getUserId());
    String previewQuery = getPreviewQueryFromJobParameters(
      !isBulkEditUpdate(jobCommand) || isNull(lastJobIdentifiersId) ? jobCommand : getJobCommandById(lastJobIdentifiersId.toString()), limit, PREVIEW_USERS_QUERY);
    return new ResponseEntity<>(userClient.getUserByQuery(previewQuery, limit), HttpStatus.OK);
  }

  @Override public ResponseEntity<ItemCollection> getPreviewItemsByJobId(UUID jobId, Integer limit) {
    var jobCommand = getJobCommandById(jobId.toString());
    var lastJobIdentifiersId = lastJobIdentifiersByCurrentUser.get(folioExecutionContext.getUserId());
    String previewQuery = getPreviewQueryFromJobParameters(
      !isBulkEditUpdate(jobCommand) || isNull(lastJobIdentifiersId) ? jobCommand : getJobCommandById(lastJobIdentifiersId.toString()), limit, PREVIEW_ITEMS_QUERY);
    return new ResponseEntity<>(inventoryClient.getItemByQuery(previewQuery, limit), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Resource> downloadPreviewByJobId(@ApiParam(value = "UUID of the JobCommand", required = true) @PathVariable("jobId") UUID jobId) {
    var jobCommand = getJobCommandById(jobId.toString());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    try {
      var updatedFileName = FilenameUtils.getName(jobCommand.getJobParameters().getString(UPDATED_FILE_NAME));
      var updatedFullPath = FilenameUtils.getFullPath(jobCommand.getJobParameters().getString(UPDATED_FILE_NAME));
      Path updatedFilePath = Paths.get(updatedFullPath + updatedFileName);
      ByteArrayResource updatedFileResource = new ByteArrayResource(Files.readAllBytes(updatedFilePath));
      headers.setContentLength(updatedFilePath.toFile().length());
      headers.setContentDispositionFormData(updatedFileName, updatedFileName);
      return ResponseEntity.ok().headers(headers).body(updatedFileResource);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(new DescriptiveResource(e.getMessage()));
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
    if (isBulkEditIdentifiers(jobCommand)) {
      lastJobIdentifiersByCurrentUser.put(folioExecutionContext.getUserId(), jobId);
    }
    var uploadedPath = Path.of(workDir, file.getOriginalFilename());

    try {
      if (Files.exists(uploadedPath)) {
        FileUtils.forceDelete(uploadedPath.toFile());
      }
      if (isBulkEditUpdate(jobCommand)) {
        processBulkEditUpdateUploadCSV(uploadedPath, jobCommand, file);
      } else {
        Files.write(uploadedPath, file.getBytes());
      }
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
      e.printStackTrace();
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
    paramsBuilder.addString(TEMP_OUTPUT_FILE_PATH,
      workDir + (isBulkEditUpdate(jobCommand) ? EMPTY : LocalDate.now() + MATCHED_RECORDS) + FilenameUtils.getBaseName(fileName));
    paramsBuilder.addString(EXPORT_TYPE, jobCommand.getExportType().getValue());
    ofNullable(jobCommand.getIdentifierType()).ifPresent(type ->
      paramsBuilder.addString("identifierType", type.getValue()));
    ofNullable(jobCommand.getEntityType()).ifPresent(type ->
      paramsBuilder.addString("entityType", type.getValue()));
    jobCommand.setJobParameters(paramsBuilder.toJobParameters());
  }

  private long countLines(Path path, boolean skipHeaders) throws IOException {
    try (var lines = Files.lines(path)) {
      return skipHeaders ? lines.count() - 1 : lines.count();
    }
  }

  private boolean isBulkEditUpdate(JobCommand jobCommand) {
    return jobCommand.getExportType() == BULK_EDIT_UPDATE;
  }

  private boolean isBulkEditIdentifiers(JobCommand jobCommand) {
    return jobCommand.getExportType() == BULK_EDIT_IDENTIFIERS;
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

  private ItemCollection prepareItemContentUpdateResponse(List<ItemFormat> itemFormats, Integer limit) {
      var items = itemFormats.stream()
        .limit(isNull(limit) ? Integer.MAX_VALUE : limit)
        .map(bulkEditParseService::mapItemFormatToItem)
        .collect(Collectors.toList());
      return new ItemCollection().items(items).totalRecords(itemFormats.size());
  }

  private String buildPreviewQueryFromJobCommand(JobCommand jobCommand, int limit) {
    switch(jobCommand.getExportType()) {
    case BULK_EDIT_UPDATE:
    case BULK_EDIT_IDENTIFIERS:
      return buildPreviewQueryFromCsv(jobCommand, limit);
    case BULK_EDIT_QUERY:
      return jobCommand.getJobParameters().getString(QUERY);
    default:
      throw new NonSupportedEntityException(format("Non-supported export type: %s", jobCommand.getExportType()));
    }
  }

  private String getPreviewQueryFromJobParameters(JobCommand jobCommand, int limit, String previewQuery) {
    String query = jobCommand.getJobParameters().getString(previewQuery);
    if (isNull(query)) {
      query = buildPreviewQueryFromJobCommand(jobCommand, limit);
      if (nonNull(query)) {
        var paramsBuilder = new JobParametersBuilder(jobCommand.getJobParameters());
        paramsBuilder.addString(previewQuery, query);
        jobCommand.setJobParameters(paramsBuilder.toJobParameters());
      }
    }
    return query;
  }

  private String buildPreviewQueryFromCsv(JobCommand jobCommand, int limit) {
    var fileName = extractFileName(jobCommand);
    try (var reader = new CSVReader(new FileReader(fileName))) {
      var values = reader.readAll().stream()
        .skip(BULK_EDIT_UPDATE == jobCommand.getExportType() ? 1 : 0)
        .limit(limit)
        .map(line -> BULK_EDIT_UPDATE == jobCommand.getExportType() ?
          extractIdentifierFromUpdateCsv(line, jobCommand) :
          extractIdentifierFromCsv(line))
        .collect(Collectors.joining(" OR "));
      return format("%s==(%s)", resolveIdentifier(jobCommand.getIdentifierType().getValue()), values);
    } catch (Exception e) {
      throw new FileOperationException(format("Failed to read %s file, reason: %s", fileName, e.getMessage()));
    }
  }

  private String extractFileName(JobCommand jobCommand) {
    return Optional.ofNullable(jobCommand.getJobParameters().getString(FILE_NAME))
      .orElseThrow(() -> new FileOperationException("File for preview is not present or was not uploaded"));
  }

  private String extractIdentifierFromCsv(String[] line) {
    return line.length > 0 ? line[0] : EMPTY;
  }

  private String extractIdentifierFromUpdateCsv(String[] line, JobCommand jobCommand) {
    var identifierIndex = getIdentifierIndex(jobCommand);
    return (line.length > identifierIndex + 1 ? line[identifierIndex] : EMPTY);
  }

  private int getIdentifierIndex(JobCommand jobCommand) {
    if (USER == jobCommand.getEntityType()) {
      return Arrays.asList(UserFormat.getUserFieldsArray()).indexOf(resolveIdentifier(jobCommand.getIdentifierType().getValue()));
    } else if (ITEM == jobCommand.getEntityType()) {
      return Arrays.asList(ItemFormat.getItemFieldsArray()).indexOf(resolveIdentifier(jobCommand.getIdentifierType().getValue()));
    } else {
      throw new NonSupportedEntityException(format("Non-supported entity type: %s", jobCommand.getEntityType()));
    }
  }

  private <T> List<T> getDifferenceBetweenInitialAndEditedRecordsCSV(InputStream edited, Class<T> clazz) {
    return new CsvToBeanBuilder<T>(new InputStreamReader(edited))
      .withType(clazz)
      .withSkipLines(1)
      .build()
      .parse()
      .stream()
      .filter(recordFormat -> {
        if (clazz == UserFormat.class) {
          return applyUserFilter((UserFormat) recordFormat);
        } else if (clazz == ItemFormat.class) {
          return applyItemFilter((ItemFormat) recordFormat);
        }
        throw new UnsupportedOperationException("Only UserFormat or ItemFormat is supported, but not " + clazz);
      })
      .collect(Collectors.toList());
  }

  private boolean applyUserFilter(UserFormat userFormat) {
    var userFromDB = userClient.getUserById(userFormat.getId());
    userFromDB.setMetadata(null); // Exclude metadata from comparing users.
    return !userFromDB.equals(bulkEditParseService.mapUserFormatToUser(userFormat));
  }

  private boolean applyItemFilter(ItemFormat itemFormat) {
    var itemFromDB = inventoryClient.getItemById(itemFormat.getId());
    itemFromDB.setMetadata(null); // Exclude metadata from comparing items.
    return !itemFromDB.equals(bulkEditParseService.mapItemFormatToItem(itemFormat));
  }

  private void processBulkEditUpdateUploadCSV(Path uploadedPath, JobCommand jobCommand, MultipartFile file) throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
    Class clazz = jobCommand.getEntityType() == ITEM ? ItemFormat.class : UserFormat.class;
    var csvLines = getDifferenceBetweenInitialAndEditedRecordsCSV(file.getInputStream(), clazz);
    if (csvLines.isEmpty()) { // If no records changed, just write column headers.
      Files.write(uploadedPath, UserFormat.getUserColumnHeaders().getBytes());
    } else {
      CsvHelper.saveRecordsToCsv(csvLines, clazz, uploadedPath.toFile().getAbsolutePath());
    }
  }
}
