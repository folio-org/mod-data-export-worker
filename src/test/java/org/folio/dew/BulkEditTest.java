package org.folio.dew;

import lombok.SneakyThrows;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.HoldingsContentUpdate;
import org.folio.dew.domain.dto.HoldingsContentUpdateCollection;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.update.BulkEditHoldingsContentUpdateService;
import org.folio.dew.utils.Constants;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.folio.dew.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.dew.domain.dto.EntityType.ITEM;
import static org.folio.dew.domain.dto.EntityType.USER;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.IdentifierType.BARCODE;
import static org.folio.dew.domain.dto.IdentifierType.HOLDINGS_RECORD_ID;
import static org.folio.dew.domain.dto.IdentifierType.ID;
import static org.folio.dew.domain.dto.IdentifierType.INSTANCE_HRID;
import static org.folio.dew.domain.dto.IdentifierType.ITEM_BARCODE;
import static org.folio.dew.domain.dto.JobParameterNames.JOB_ID;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.BULKEDIT_DIR_NAME;
import static org.folio.dew.utils.Constants.ENTITY_TYPE;
import static org.folio.dew.utils.Constants.EXPORT_TYPE;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.IDENTIFIER_TYPE;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.ROLLBACK_FILE;
import static org.folio.dew.utils.Constants.TEMP_IDENTIFIERS_FILE_NAME;
import static org.folio.dew.utils.Constants.TOTAL_CSV_LINES;
import static org.folio.dew.utils.Constants.getWorkingDirectory;
import static org.folio.dew.utils.CsvHelper.countLines;
import static org.folio.dew.utils.SystemHelper.getTempDirWithSeparatorSuffix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.batch.test.AssertFile.assertFileEquals;

@ExtendWith(MockitoExtension.class)
class BulkEditTest extends BaseBatchTest {

  private static final String HOLDINGS_IDENTIFIERS_CSV = "src/test/resources/upload/holdings_identifiers.csv";
  private static final String HOLDINGS_IDENTIFIERS_BAD_REFERENCE_IDS_CSV = "src/test/resources/upload/holdings_identifiers_for_bad_reference_ids.csv";
  private static final String ITEM_BARCODE_FOR_HOLDINGS_IDENTIFIERS_CSV = "src/test/resources/upload/item_barcode_for_holdings_identifiers.csv";
  private static final String EXPECTED_HOLDINGS_OUTPUT_BAD_REFERENCE_CSV = "src/test/resources/output/bulk_edit_holdings_records_reference_not_found.csv";
  private static final String HOLDINGS_IDENTIFIERS_EMPTY_REFERENCE_IDS_CSV = "src/test/resources/upload/holdings_identifiers_empty_reference_ids.csv";
  private static final String EXPECTED_HOLDINGS_OUTPUT_EMPTY_REFERENCE_CSV = "src/test/resources/output/bulk_edit_holdings_records_empty_reference.csv";
  private static final String ITEM_IDENTIFIERS_BAD_REFERENCE_IDS_CSV = "src/test/resources/upload/item_identifiers_bad_reference.csv";
  private static final String EXPECTED_ITEMS_OUTPUT_BAD_REFERENCE_CSV = "src/test/resources/output/bulk_edit_items_reference_not_found.csv";
  private final static String EXPECTED_ITEM_OUTPUT_BAD_REFERENCE_ERRORS = "src/test/resources/output/bulk_edit_items_bad_reference_errors.csv";
  private static final String ITEM_IDENTIFIERS_EMPTY_REFERENCE_IDS_CSV = "src/test/resources/upload/item_identifiers_empty_reference.csv";
  private static final String EXPECTED_ITEM_OUTPUT_EMPTY_REFERENCE_CSV = "src/test/resources/output/bulk_edit_items_empty_reference.csv";
  private static final String USER_IDENTIFIERS_BAD_REFERENCE_IDS_CSV = "src/test/resources/upload/user_identifiers_bad_reference.csv";
  private static final String EXPECTED_USER_OUTPUT_BAD_REFERENCE_CSV = "src/test/resources/output/bulk_edit_users_reference_not_found.csv";
  private final static String EXPECTED_USER_OUTPUT_BAD_REFERENCE_ERRORS = "src/test/resources/output/bulk_edit_users_bad_reference_errors.csv";
  private static final String USER_IDENTIFIERS_EMPTY_REFERENCE_IDS_CSV = "src/test/resources/upload/user_identifiers_empty_reference.csv";
  private static final String EXPECTED_USER_OUTPUT_EMPTY_REFERENCE_CSV = "src/test/resources/output/bulk_edit_users_empty_reference.csv";
  private static final String BARCODES_CSV = "src/test/resources/upload/barcodes.csv";
  private static final String BARCODES_FOR_PROGRESS_CSV = "src/test/resources/upload/barcodes_for_progress.csv";
  private static final String ITEM_BARCODES_CSV = "src/test/resources/upload/item_barcodes.csv";
  private static final String ITEM_BARCODES_DOUBLE_QOUTES_CSV = "src/test/resources/upload/item_barcodes_double_qoutes.csv";
  private static final String ITEM_HOLDINGS_CSV = "src/test/resources/upload/item_holdings.csv";
  private static final String USER_RECORD_CSV = "src/test/resources/upload/bulk_edit_user_record.csv";
  private static final String ITEM_RECORD_CSV = "src/test/resources/upload/bulk_edit_item_record.csv";
  private static final String USER_RECORD_CSV_NOT_FOUND = "src/test/resources/upload/bulk_edit_user_record_not_found.csv";
  private static final String ITEM_RECORD_CSV_NOT_FOUND = "src/test/resources/upload/bulk_edit_item_record_not_found.csv";
  private static final String USER_RECORD_CSV_BAD_CONTENT = "src/test/resources/upload/bulk_edit_user_record_bad_content.csv";
  private static final String USER_RECORD_CSV_BAD_CUSTOM_FIELD = "src/test/resources/upload/bulk_edit_user_record_bad_custom_field.csv";
  private static final String USER_RECORD_CSV_EMPTY_PATRON_GROUP = "src/test/resources/upload/bulk_edit_user_record_empty_patron_group.csv";
  private static final String ITEM_RECORD_CSV_INVALID_NOTES = "src/test/resources/upload/bulk_edit_item_record_invalid_notes.csv";
  private static final String ITEM_RECORD_CSV_INVALID_CIRCULATION_NOTES = "src/test/resources/upload/bulk_edit_item_record_invalid_circulation_notes.csv";
  private static final String ITEM_RECORD_IN_APP_UPDATED = "src/test/resources/upload/bulk_edit_item_record_in_app_updated.csv";
  private static final String ITEM_RECORD_IN_APP_UPDATED_COPY = "storage/bulk_edit_item_record_in_app_updated.csv";
  private static final String HOLDINGS_RECORD_IN_APP_UPDATED = "src/test/resources/upload/bulk_edit_holdings_record_in_app_updated.csv";
  private static final String USER_RECORD_ROLLBACK_CSV = "test-directory/bulk_edit_rollback.csv";
  private static final String BARCODES_SOME_NOT_FOUND = "src/test/resources/upload/barcodesSomeNotFound.csv";
  private static final String ITEM_BARCODES_SOME_NOT_FOUND = "src/test/resources/upload/item_barcodes_some_not_found.csv";
  private static final String USERS_QUERY_FILE_PATH = "src/test/resources/upload/users_by_group.cql";
  private static final String ITEMS_QUERY_FILE_PATH = "src/test/resources/upload/items_by_barcode.cql";
  private static final String QUERY_NO_GROUP_FILE_PATH = "src/test/resources/upload/active_no_group.cql";
  private static final String EXPECTED_BULK_EDIT_USER_OUTPUT = "src/test/resources/output/bulk_edit_user_identifiers_output.csv";
  private static final String EXPECTED_BULK_EDIT_USER_JSON_OUTPUT = "src/test/resources/output/bulk_edit_user_identifiers_json_output.json";
  private static final String EXPECTED_BULK_EDIT_ITEM_OUTPUT = "src/test/resources/output/bulk_edit_item_identifiers_output.csv";
  private static final String EXPECTED_BULK_EDIT_ITEM_JSON_OUTPUT = "src/test/resources/output/bulk_edit_item_identifiers_json_output.json";
  private static final String EXPECTED_BULK_EDIT_ITEM_QUERY_JSON_OUTPUT = "src/test/resources/output/bulk_edit_item_query_json_output.json";

  private static final String EXPECTED_BULK_EDIT_HOLDINGS_OUTPUT = "src/test/resources/output/bulk_edit_holdings_records_output.csv";
  private static final String EXPECTED_BULK_EDIT_HOLDINGS_JSON_OUTPUT = "src/test/resources/output/bulk_edit_holdings_records_json_output.json";

  private static final String EXPECTED_BULK_EDIT_HOLDINGS_OUTPUT_INST_HRID = "src/test/resources/output/bulk_edit_holdings_records_output_instance_hrid.csv";
  private static final String EXPECTED_BULK_EDIT_HOLDINGS_OUTPUT_ITEM_BARCODE = "src/test/resources/output/bulk_edit_holdings_records_output_item_barcode.csv";
  private static final String EXPECTED_BULK_EDIT_ITEM_OUTPUT_ESCAPED = "src/test/resources/output/bulk_edit_item_identifiers_output_escaped.csv";
  private static final String EXPECTED_NO_GROUP_OUTPUT = "src/test/resources/output/bulk_edit_no_group_output.csv";
  private static final String EXPECTED_ITEMS_QUERY_OUTPUT = "src/test/resources/output/bulk_edit_item_query_output.csv";
  private final static String EXPECTED_BULK_EDIT_OUTPUT_SOME_NOT_FOUND = "src/test/resources/output/bulk_edit_user_identifiers_output_some_not_found.csv";
  private final static String EXPECTED_BULK_EDIT_ITEM_OUTPUT_SOME_NOT_FOUND = "src/test/resources/output/bulk_edit_item_identifiers_output_some_not_found.csv";
  private final static String EXPECTED_BULK_EDIT_OUTPUT_ERRORS = "src/test/resources/output/bulk_edit_user_identifiers_errors_output.csv";
  private final static String EXPECTED_BULK_EDIT_ITEM_OUTPUT_ERRORS = "src/test/resources/output/bulk_edit_item_identifiers_errors_output.csv";
  private final static String EXPECTED_BULK_EDIT_HOLDINGS_ERRORS = "src/test/resources/output/bulk_edit_holdings_records_errors_output.csv";
  private final static String EXPECTED_BULK_EDIT_HOLDINGS_BAD_REFERENCE_IDS_ERRORS = "src/test/resources/output/bulk_edit_holdings_records_bad_reference_ids_errors_output.csv";
  private final static String EXPECTED_BULK_EDIT_HOLDINGS_ERRORS_INST_HRID = "src/test/resources/output/bulk_edit_holdings_records_errors_output_inst_hrid.csv";
  private static final String EXPECTED_BULK_EDIT_HOLDINGS_ERRORS_ITEM_BARCODE = "src/test/resources/output/bulk_edit_holdings_records_errors_output_item_barcode.csv";
  private final static String EXPECTED_BULK_EDIT_ITEM_IDENTIFIERS_HOLDINGS_ERRORS_OUTPUT = "src/test/resources/output/bulk_edit_item_identifiers_holdings_errors_output.csv";
  private final static String ITEM_NO_VERSION = "src/test/resources/upload/bulk_edit_item_record_no_version.csv";
  private final static String HOLDINGS_RECORD_NO_VERSION = "src/test/resources/upload/bulk_edit_holdings_record_no_version.csv";
  private final static String HOLDINGS_RECORD_BY_ITEM_BARCODE_IN_APP_UPDATED_NO_CHANGE = "src/test/resources/upload/bulk_edit_holdings_record_by_item_barcode_in_app_updated_no_change.csv";
  private final static String ERROR_HOLDINGS_BY_ITEM_BARCODE_NO_CHANGE = "src/test/resources/output/expected_error_holdings_by_item_barcode_no_change.csv";

  @Autowired
  private Job bulkEditProcessUserIdentifiersJob;
  @Autowired
  private Job bulkEditProcessItemIdentifiersJob;

  @Autowired
  private Job bulkEditProcessHoldingsIdentifiersJob;
  @Autowired
  private Job bulkEditUserCqlJob;
  @Autowired
  private Job bulkEditItemCqlJob;
  @Autowired
  private Job bulkEditUpdateUserRecordsJob;
  @Autowired
  private Job bulkEditUpdateItemRecordsJob;
  @Autowired
  private Job bulkEditRollBackJob;
  @Autowired
  private Job bulkEditUpdateHoldingsRecordsJob;
  @Autowired
  private BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  @Autowired
  private LocalFilesStorage localFilesStorage;

  @Autowired
  private BulkEditHoldingsContentUpdateService bulkEditHoldingsContentUpdateService;

  @MockBean
  private KafkaService kafkaService;

  @Test
  @DisplayName("Run bulk-edit (user identifiers) successfully")
  void uploadUserIdentifiersJobTest() throws Exception {

    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessUserIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, USER, BARCODE, BARCODES_CSV);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyCsvAndJsonOutput(jobExecution, EXPECTED_BULK_EDIT_USER_OUTPUT, EXPECTED_BULK_EDIT_USER_JSON_OUTPUT);
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Update retrieval progress (user identifiers) successfully")
  void shouldUpdateProgressUponUserIdentifiersJob() throws Exception {

    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessUserIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, USER, BARCODE, BARCODES_FOR_PROGRESS_CSV);
    testLauncher.launchJob(jobParameters);

    var jobCaptor = ArgumentCaptor.forClass(org.folio.de.entity.Job.class);

    // expected 4 events: 1st - job started, 2nd, 3rd - updates after each chunk (100 identifiers), 4th - job completed
    Mockito.verify(kafkaService, Mockito.times(4)).send(Mockito.any(), Mockito.any(), jobCaptor.capture());

    verifyJobProgressUpdates(jobCaptor);
  }

  @Test
  @DisplayName("Update retrieval progress (item identifiers) successfully")
  void shouldUpdateProgressUponItemIdentifiersJob() throws Exception {

    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessItemIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, ITEM, BARCODE, BARCODES_FOR_PROGRESS_CSV);
    testLauncher.launchJob(jobParameters);

    var jobCaptor = ArgumentCaptor.forClass(org.folio.de.entity.Job.class);

    // expected 4 events: 1st - job started, 2nd, 3rd - updates after each chunk (100 identifiers), 4th - job completed
    Mockito.verify(kafkaService, Mockito.times(4)).send(Mockito.any(), Mockito.any(), jobCaptor.capture());

    verifyJobProgressUpdates(jobCaptor);
  }

  @ParameterizedTest
  @EnumSource(value = IdentifierType.class, names = {"USER_NAME", "EXTERNAL_SYSTEM_ID", "INSTANCE_HRID", "ITEM_BARCODE"}, mode = EnumSource.Mode.EXCLUDE)
  @DisplayName("Run bulk-edit (item identifiers) successfully")
  void uploadItemIdentifiersJobTest(IdentifierType identifierType) throws Exception {

    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessItemIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, ITEM, identifierType, ITEM_BARCODES_CSV);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyCsvAndJsonOutput(jobExecution, EXPECTED_BULK_EDIT_ITEM_OUTPUT, EXPECTED_BULK_EDIT_ITEM_JSON_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Run bulk-edit (item identifiers) with wrong reference identifiers")
  void shouldWriteErrorsWhenItemReferenceDataNotFoundAndContinueBulkEdit() throws Exception {

    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessItemIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, ITEM, BARCODE, ITEM_IDENTIFIERS_BAD_REFERENCE_IDS_CSV);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFilesOutput(jobExecution, EXPECTED_ITEMS_OUTPUT_BAD_REFERENCE_CSV, EXPECTED_ITEM_OUTPUT_BAD_REFERENCE_ERRORS);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Run bulk-edit (item identifiers) with empty reference identifiers")
  void shouldSkipEmptyItemReferenceData() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessItemIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, ITEM, BARCODE, ITEM_IDENTIFIERS_EMPTY_REFERENCE_IDS_CSV);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFilesOutput(jobExecution, EXPECTED_ITEM_OUTPUT_EMPTY_REFERENCE_CSV, null);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Run bulk-edit (user identifiers) with wrong reference identifiers")
  void shouldWriteErrorsWhenUserReferenceDataNotFoundAndContinueBulkEdit() throws Exception {

    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessUserIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, USER, BARCODE, USER_IDENTIFIERS_BAD_REFERENCE_IDS_CSV);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFilesOutput(jobExecution, EXPECTED_USER_OUTPUT_BAD_REFERENCE_CSV, EXPECTED_USER_OUTPUT_BAD_REFERENCE_ERRORS);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Run bulk-edit (user identifiers) with empty reference identifiers")
  void shouldSkipEmptyUserReferenceData() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessUserIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, USER, BARCODE, USER_IDENTIFIERS_EMPTY_REFERENCE_IDS_CSV);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFilesOutput(jobExecution, EXPECTED_USER_OUTPUT_EMPTY_REFERENCE_CSV, null);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @ParameterizedTest
  @EnumSource(value = IdentifierType.class, names = {"ID", "HRID", "INSTANCE_HRID", "ITEM_BARCODE"}, mode = EnumSource.Mode.INCLUDE)
  @DisplayName("Run bulk-edit (holdings records identifiers) successfully")
  void uploadHoldingsIdentifiersJobTest(IdentifierType identifierType) throws Exception {

    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessHoldingsIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, HOLDINGS_RECORD, identifierType, HOLDINGS_IDENTIFIERS_CSV);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    String expectedErrorsOutputFilePath;
    if (INSTANCE_HRID == identifierType) {
      expectedErrorsOutputFilePath = EXPECTED_BULK_EDIT_HOLDINGS_ERRORS_INST_HRID;
      verifyFilesOutput(jobExecution, EXPECTED_BULK_EDIT_HOLDINGS_OUTPUT_INST_HRID, expectedErrorsOutputFilePath);
    } else if (ITEM_BARCODE == identifierType) {
      expectedErrorsOutputFilePath = EXPECTED_BULK_EDIT_HOLDINGS_ERRORS_ITEM_BARCODE;
      verifyFilesOutput(jobExecution, EXPECTED_BULK_EDIT_HOLDINGS_OUTPUT_ITEM_BARCODE, expectedErrorsOutputFilePath);
    } else {
      expectedErrorsOutputFilePath = EXPECTED_BULK_EDIT_HOLDINGS_ERRORS;
      verifyFilesOutput(jobExecution, EXPECTED_BULK_EDIT_HOLDINGS_OUTPUT, expectedErrorsOutputFilePath);
      verifyCsvAndJsonOutput(jobExecution, EXPECTED_BULK_EDIT_HOLDINGS_OUTPUT, EXPECTED_BULK_EDIT_HOLDINGS_JSON_OUTPUT);
    }

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Run bulk-edit (holdings records identifiers) with wrong reference identifiers")
  void shouldWriteErrorsWhenHoldingsReferenceDataNotFoundAndContinueBulkEdit() throws Exception {

    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessHoldingsIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, HOLDINGS_RECORD, ID, HOLDINGS_IDENTIFIERS_BAD_REFERENCE_IDS_CSV);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFilesOutput(jobExecution, EXPECTED_HOLDINGS_OUTPUT_BAD_REFERENCE_CSV, EXPECTED_BULK_EDIT_HOLDINGS_BAD_REFERENCE_IDS_ERRORS);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Run bulk-edit (holdings records identifiers) with empty reference identifiers")
  void shouldSkipEmptyHoldingsReferenceData() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessHoldingsIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, HOLDINGS_RECORD, ID, HOLDINGS_IDENTIFIERS_EMPTY_REFERENCE_IDS_CSV);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFilesOutput(jobExecution, EXPECTED_HOLDINGS_OUTPUT_EMPTY_REFERENCE_CSV, null);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Upload item identifiers (holdingsRecordId) successfully")
  void shouldProcessMultipleItemsOnHoldingsRecordId() throws Exception {

    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessItemIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, ITEM, HOLDINGS_RECORD_ID, ITEM_HOLDINGS_CSV);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFilesOutput(jobExecution, EXPECTED_BULK_EDIT_ITEM_OUTPUT, EXPECTED_BULK_EDIT_ITEM_IDENTIFIERS_HOLDINGS_ERRORS_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Run bulk-edit (user identifiers) with errors")
  void bulkEditUserJobTestWithErrors() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessUserIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, USER, BARCODE, BARCODES_SOME_NOT_FOUND);

    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFilesOutput(jobExecution, EXPECTED_BULK_EDIT_OUTPUT_SOME_NOT_FOUND);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Run bulk-edit (item identifiers) with errors")
  void bulkEditItemJobTestWithErrors() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessItemIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, ITEM, BARCODE, ITEM_BARCODES_SOME_NOT_FOUND);

    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFilesOutput(jobExecution, EXPECTED_BULK_EDIT_ITEM_OUTPUT_SOME_NOT_FOUND);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Run bulk-edit (user query) successfully")
  void bulkEditUserQueryJobTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditUserCqlJob);

    final JobParameters jobParameters = prepareJobParameters(ExportType.BULK_EDIT_QUERY, USER, BARCODE, USERS_QUERY_FILE_PATH);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyCsvAndJsonOutput(jobExecution, EXPECTED_BULK_EDIT_USER_OUTPUT, EXPECTED_BULK_EDIT_USER_JSON_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Process users without patron group id successfully")
  void shouldProcessUsersWithoutPatronGroupIdSuccessfully() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditUserCqlJob);

    final JobParameters jobParameters = prepareJobParameters(ExportType.BULK_EDIT_QUERY, USER, BARCODE, QUERY_NO_GROUP_FILE_PATH);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFilesOutput(jobExecution, EXPECTED_NO_GROUP_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Run bulk-edit (item query) successfully")
  void bulkEditItemQueryJobTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditItemCqlJob);

    final JobParameters jobParameters = prepareJobParameters(ExportType.BULK_EDIT_QUERY, ITEM, BARCODE, ITEMS_QUERY_FILE_PATH);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyCsvAndJsonOutput(jobExecution, EXPECTED_ITEMS_QUERY_OUTPUT, EXPECTED_BULK_EDIT_ITEM_QUERY_JSON_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Disabled
  // TODO uncomment when resolved
  @ParameterizedTest
  @ValueSource(strings = {USER_RECORD_CSV, USER_RECORD_CSV_NOT_FOUND, USER_RECORD_CSV_BAD_CONTENT, USER_RECORD_CSV_BAD_CUSTOM_FIELD, USER_RECORD_CSV_EMPTY_PATRON_GROUP})
  @DisplayName("Run update user records w/ and w/o errors")
  void uploadUserRecordsJobTest(String csvFileName) throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditUpdateUserRecordsJob);
    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_UPDATE, USER, BARCODE, csvFileName);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    var errors = bulkEditProcessingErrorsService.readErrorsFromCSV(jobExecution.getJobParameters().getString("jobId"), csvFileName, 10);

    if (!USER_RECORD_CSV.equals(csvFileName)) {
      if (USER_RECORD_CSV_BAD_CUSTOM_FIELD.equals(csvFileName)) {
        assertThat(errors.getErrors()).hasSize(2);
      } else {
        assertThat(errors.getErrors()).hasSize(1);
      }
      assertThat(jobExecution.getExecutionContext().getString(OUTPUT_FILES_IN_STORAGE)).isNotEmpty();
    } else {
      assertThat(jobExecution.getExecutionContext().getString(OUTPUT_FILES_IN_STORAGE)).isNotEmpty();
      assertThat(errors.getErrors()).isEmpty();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {ITEM_RECORD_CSV, ITEM_RECORD_CSV_NOT_FOUND,
    ITEM_RECORD_CSV_INVALID_NOTES, ITEM_RECORD_CSV_INVALID_CIRCULATION_NOTES})
  @DisplayName("Run update item records w/ and w/o errors")
  void uploadItemRecordsJobTest(String csvFileName) throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditUpdateItemRecordsJob);
    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_UPDATE, ITEM, BARCODE, csvFileName);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    var errors = bulkEditProcessingErrorsService.readErrorsFromCSV(jobExecution.getJobParameters().getString("jobId"), csvFileName, 10);
    if (!ITEM_RECORD_CSV.equals(csvFileName)) {
      assertThat(errors.getErrors()).hasSize(1);
      assertThat(jobExecution.getExecutionContext().getString(OUTPUT_FILES_IN_STORAGE)).isNotEmpty();
    } else {
      assertThat(errors.getErrors()).isEmpty();
      assertThat(jobExecution.getExecutionContext().getString(OUTPUT_FILES_IN_STORAGE)).isNotEmpty();
    }
  }

  @Test
  @DisplayName("Run item records update when in-app updates available - successful")
  @SneakyThrows
  void shouldUseItemsInAppUpdatesFileIfPresent() {
    // create a copy of file since it will be deleted and consequent test runs will fail
    localFilesStorage.write(ITEM_RECORD_IN_APP_UPDATED_COPY, Files.readAllBytes(new File(ITEM_RECORD_IN_APP_UPDATED).toPath()));

    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditUpdateItemRecordsJob);
    var builder = new JobParametersBuilder(prepareJobParameters(BULK_EDIT_UPDATE, ITEM, BARCODE, ITEM_RECORD_CSV));
    builder.addString(UPDATED_FILE_NAME, ITEM_RECORD_IN_APP_UPDATED_COPY);
    JobExecution jobExecution = testLauncher.launchJob(builder.toJobParameters());

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    var request = wireMockServer.getAllServeEvents().get(0).getRequest();
    assertThat(request.getMethod().getName()).isEqualTo("PUT");
    assertThat(request.getUrl()).isEqualTo("/inventory/items/8a29baff-b703-4c3a-8b7b-ef476b2cd583");
  }

  @Test
  @DisplayName("Run holdings records in-app update - successful")
  @SneakyThrows
  void shouldRunHoldingsInAppUpdateJob() {
    var jobId = UUID.randomUUID().toString();
    var fileName = jobId + PATH_SEPARATOR + FilenameUtils.getName(HOLDINGS_RECORD_IN_APP_UPDATED);
    remoteFilesStorage.upload(fileName, HOLDINGS_RECORD_IN_APP_UPDATED);
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditUpdateHoldingsRecordsJob);
    var jobParameters = new JobParametersBuilder()
      .addString(JOB_ID, jobId)
      .addString(EXPORT_TYPE, BULK_EDIT_UPDATE.getValue())
      .addString(ENTITY_TYPE, HOLDINGS_RECORD.getValue())
      .addString(IDENTIFIER_TYPE, ID.getValue())
      .addString(UPDATED_FILE_NAME, fileName)
      .toJobParameters();

    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    assertNotNull(jobExecution.getExecutionContext().getString(OUTPUT_FILES_IN_STORAGE));

    var request = wireMockServer.getAllServeEvents().get(0).getRequest();
    assertThat(request.getMethod().getName()).isEqualTo("PUT");
    assertThat(request.getUrl()).isEqualTo("/holdings-storage/holdings/0b1e3760-f689-493e-a98e-9cc9dadb7e83");
  }

  @Test
  @DisplayName("Run holdings records in-app update - errors file shows identifier")
  @SneakyThrows
  void shouldRunHoldingsInAppUpdateJobAndShowIdentifierWhenHoldingsFoundByItemBarcode() {

    // Bulk edit identifiers part.
    var testLauncher = createTestLauncher(bulkEditProcessHoldingsIdentifiersJob);

    var jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, HOLDINGS_RECORD, ITEM_BARCODE, ITEM_BARCODE_FOR_HOLDINGS_IDENTIFIERS_CSV);
    var jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    var jobId = UUID.randomUUID().toString();
    var fileName = jobId + PATH_SEPARATOR + FilenameUtils.getName(HOLDINGS_RECORD_BY_ITEM_BARCODE_IN_APP_UPDATED_NO_CHANGE);
    remoteFilesStorage.upload(fileName, HOLDINGS_RECORD_BY_ITEM_BARCODE_IN_APP_UPDATED_NO_CHANGE);

    // Bulk edit update part.
    testLauncher = createTestLauncher(bulkEditUpdateHoldingsRecordsJob);
    jobParameters = new JobParametersBuilder()
      .addString(JOB_ID, jobId)
      .addString(EXPORT_TYPE, BULK_EDIT_UPDATE.getValue())
      .addString(ENTITY_TYPE, HOLDINGS_RECORD.getValue())
      .addString(IDENTIFIER_TYPE, ITEM_BARCODE.getValue())
      .addString(UPDATED_FILE_NAME, fileName)
      .addString(FILE_NAME, ITEM_BARCODE_FOR_HOLDINGS_IDENTIFIERS_CSV)
      .addString(TEMP_OUTPUT_FILE_PATH, fileName.replaceAll(".csv", ""))
      .toJobParameters();

    JobCommand jobCommand = new JobCommand();
    jobCommand.setId(UUID.fromString(jobId));
    jobCommand.setJobParameters(jobParameters);

    // Holdings record has the Annex permanent location, so no change in value needed.
    bulkEditHoldingsContentUpdateService.process(jobCommand, new HoldingsContentUpdateCollection()
      .holdingsContentUpdates(List.of(new HoldingsContentUpdate().option(HoldingsContentUpdate.OptionEnum.PERMANENT_LOCATION)
            .action(HoldingsContentUpdate.ActionEnum.REPLACE_WITH).value("Annex"))));

    jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    var executionContext = jobExecution.getExecutionContext();
    var fileInStorage = (String) executionContext.get("outputFilesInStorage");
    String[] links = fileInStorage.split(";");

    // Make sure there is a link to error file.
    assertThat(links).hasSize(2);

    var actualResult = actualFileOutput(links[1]);
    var expectedResult = new FileSystemResource(ERROR_HOLDINGS_BY_ITEM_BARCODE_NO_CHANGE);

    assertFileEquals(expectedResult, actualResult);
  }

  @Disabled
  // TODO uncomment when resolved
  @Test
  @DisplayName("Run rollback user records successfully")
  void rollBackUserRecordsJobTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditRollBackJob);
    localFilesStorage.write(USER_RECORD_ROLLBACK_CSV, Files.readAllBytes(new File(USER_RECORD_CSV).toPath()));
    var parametersBuilder = new JobParametersBuilder();
    parametersBuilder.addString(Constants.JOB_ID, "74914e57-3406-4757-938b-9a3f718d0ee6");
    parametersBuilder.addString(FILE_NAME, USER_RECORD_ROLLBACK_CSV);
    JobExecution jobExecution = testLauncher.launchJob(parametersBuilder.toJobParameters());
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Double quotes in data should be escaped")
  @SneakyThrows
  void shouldEscapeDoubleQuotes() {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditProcessItemIdentifiersJob);

    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_IDENTIFIERS, ITEM, BARCODE, ITEM_BARCODES_DOUBLE_QOUTES_CSV);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFilesOutput(jobExecution, EXPECTED_BULK_EDIT_ITEM_OUTPUT_ESCAPED);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("_version field should be absent in item PUT body if its value is empty")
  @SneakyThrows
  void emptyVersionFieldShouldBeAbsentInItemUpdateRequestBody() {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditUpdateItemRecordsJob);
    final JobParameters jobParameters = prepareJobParameters(BULK_EDIT_UPDATE, ITEM, BARCODE, ITEM_NO_VERSION);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    assertThat(wireMockServer.getAllServeEvents().get(0).getRequest().getMethod().getName()).isEqualTo("PUT");
    assertFalse(wireMockServer.getAllServeEvents().get(0).getRequest().getBodyAsString().contains("_version"));
  }

  @Test
  @DisplayName("_version field should be absent in holdings record PUT body if its value is empty")
  @SneakyThrows
  void emptyVersionFieldShouldBeAbsentInHoldingsUpdateRequestBody() {
    var fileName = FilenameUtils.getName(HOLDINGS_RECORD_NO_VERSION);
    remoteFilesStorage.upload(fileName, HOLDINGS_RECORD_NO_VERSION);
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditUpdateHoldingsRecordsJob);
    final JobParameters jobParameters = new JobParametersBuilder()
      .addString(JOB_ID, UUID.randomUUID().toString())
      .addString(UPDATED_FILE_NAME, fileName)
      .toJobParameters();
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    assertThat(wireMockServer.getAllServeEvents().get(0).getRequest().getMethod().getName()).isEqualTo("PUT");
    assertFalse(wireMockServer.getAllServeEvents().get(0).getRequest().getBodyAsString().contains("_version"));
  }

  @SneakyThrows
  private void verifyFilesOutput(JobExecution jobExecution, String output) {
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    String fileInStorage = (String) executionContext.get("outputFilesInStorage");
    if (fileInStorage.contains(";")) {
      String[] links = fileInStorage.split(";");
      fileInStorage = links[0];
      String errorInStorage = links[1];
      if (StringUtils.isNotEmpty(errorInStorage)){
        final FileSystemResource actualResultWithErrors = actualFileOutput(errorInStorage);
        final FileSystemResource expectedResultWithErrors = jobExecution.getJobInstance().getJobName().contains("-USER") ?
          new FileSystemResource(EXPECTED_BULK_EDIT_OUTPUT_ERRORS) :
          new FileSystemResource(EXPECTED_BULK_EDIT_ITEM_OUTPUT_ERRORS);
        assertFileEquals(expectedResultWithErrors, actualResultWithErrors);
      }
    }
    final FileSystemResource actualResult = actualFileOutput(fileInStorage);
    FileSystemResource expectedCharges = new FileSystemResource(output);
    assertFileEquals(expectedCharges, actualResult);
  }

  @SneakyThrows
  private void verifyCsvAndJsonOutput(JobExecution jobExecution, String output, String outputJsonPath) {
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    String fileInStorage = (String) executionContext.get("outputFilesInStorage");
    String[] links = fileInStorage.split(";");
    fileInStorage = links[0];

    FileSystemResource expectedJsonFile = new FileSystemResource(outputJsonPath);
    final FileSystemResource actualJsonResult = actualFileOutput(links[2]);
    assertFileEquals(expectedJsonFile, actualJsonResult);

    final FileSystemResource actualResult = actualFileOutput(fileInStorage);
    FileSystemResource expectedCharges = new FileSystemResource(output);
    assertFileEquals(expectedCharges, actualResult);
  }

  @SneakyThrows
  private void verifyFilesOutput(JobExecution jobExecution, String output, String expectedErrorOutput) {
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    String fileInStorage = (String) executionContext.get("outputFilesInStorage");
    String[] links = fileInStorage.split(";");
    fileInStorage = links[0];
    if (Objects.isNull(expectedErrorOutput)) {
      assertEquals(3, links.length);
    } else {
      String errorInStorage = links[1];
      final FileSystemResource actualResultWithErrors = actualFileOutput(errorInStorage);
      final FileSystemResource expectedResultWithErrors = new FileSystemResource(expectedErrorOutput);
      assertFileEquals(expectedResultWithErrors, actualResultWithErrors);
    }
    if (isEmpty(fileInStorage)) {
      assertTrue(isEmpty(output));
    } else {
      final FileSystemResource actualResult = actualFileOutput(fileInStorage);
      FileSystemResource expectedCharges = new FileSystemResource(output);
      assertFileEquals(expectedCharges, actualResult);
    }
  }

  @SneakyThrows
  private JobParameters prepareJobParameters(ExportType exportType, EntityType entityType, IdentifierType identifierType, String path) {
    var parametersBuilder = new JobParametersBuilder();
    String jobId = UUID.randomUUID().toString();
    String workDir = getWorkingDirectory(springApplicationName, BULKEDIT_DIR_NAME);
    parametersBuilder.addString(TEMP_OUTPUT_FILE_PATH, workDir + jobId + "/" + "out");
    parametersBuilder.addString(TEMP_LOCAL_FILE_PATH,
      getTempDirWithSeparatorSuffix() + springApplicationName + PATH_SEPARATOR + jobId + PATH_SEPARATOR + "out");
    try {
      localFilesStorage.write(workDir + "out", new byte[0]);
      localFilesStorage.write(workDir + "out.csv", new byte[0]);
    } catch (Exception e) {
      fail(e.getMessage());
    }
    Path of = Path.of(path);
    if (BULK_EDIT_UPDATE == exportType) {
      parametersBuilder.addString(ROLLBACK_FILE, "rollback/file/path");
      // Put file on MinIO FS
      var fsPath = getWorkingDirectory(springApplicationName, BULKEDIT_DIR_NAME) + FileNameUtils.getBaseName(path) + "E" + FileNameUtils.getExtension(path);
      localFilesStorage.write(fsPath, Files.readAllBytes(of));
      parametersBuilder.addString(FILE_NAME, fsPath);
    } else if (ExportType.BULK_EDIT_QUERY == exportType) {
      parametersBuilder.addString("query", readQueryString(path));
    } else if (BULK_EDIT_IDENTIFIERS == exportType) {
      var file = getWorkingDirectory("mod-data-export-worker", BULKEDIT_DIR_NAME)  +  FileNameUtils.getBaseName(path) + "E" + FileNameUtils.getExtension(path);
      parametersBuilder.addString(FILE_NAME, file);
      localFilesStorage.write(file, Files.readAllBytes(of));
      parametersBuilder.addLong(TOTAL_CSV_LINES, countLines(localFilesStorage, file, false), false);
    }

    var tempDir = getTempDirWithSeparatorSuffix() + springApplicationName + PATH_SEPARATOR + jobId;
    var tempFile = tempDir + PATH_SEPARATOR + of.getFileName();
    Files.createDirectories(Path.of(tempDir));
    Files.write(Path.of(tempFile), Files.readAllBytes(of));
    parametersBuilder.addString(TEMP_IDENTIFIERS_FILE_NAME, tempFile);

    parametersBuilder.addString(JobParameterNames.JOB_ID, jobId);
    parametersBuilder.addString(EXPORT_TYPE, exportType.getValue());
    parametersBuilder.addString(ENTITY_TYPE, entityType.getValue());
    parametersBuilder.addString(IDENTIFIER_TYPE, identifierType.getValue());

    return parametersBuilder.toJobParameters();
  }

  @SneakyThrows
  private String readQueryString(String path) {
    try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
      return reader.readLine();
    }
  }

  private void verifyJobProgressUpdates(ArgumentCaptor<org.folio.de.entity.Job> jobCaptor) {
    var job = jobCaptor.getAllValues().get(1);
    assertThat(job.getProgress().getTotal()).isEqualTo(179);
    assertThat(job.getProgress().getProcessed()).isEqualTo(100);
    assertThat(job.getProgress().getProgress()).isEqualTo(45);
    assertThat(job.getProgress().getSuccess()).isEqualTo(80);
    assertThat(job.getProgress().getErrors()).isZero();

    job = jobCaptor.getAllValues().get(2);
    assertThat(job.getProgress().getTotal()).isEqualTo(179);
    assertThat(job.getProgress().getProcessed()).isEqualTo(179);
    assertThat(job.getProgress().getProgress()).isEqualTo(90);
    assertThat(job.getProgress().getSuccess()).isEqualTo(144);
    assertThat(job.getProgress().getErrors()).isZero();

    job = jobCaptor.getAllValues().get(3);
    assertThat(job.getProgress().getTotal()).isEqualTo(179);
    assertThat(job.getProgress().getProcessed()).isEqualTo(179);
    assertThat(job.getProgress().getProgress()).isEqualTo(100);
    assertThat(job.getProgress().getSuccess()).isEqualTo(144);
    assertThat(job.getProgress().getErrors()).isEqualTo(35);
  }

}
