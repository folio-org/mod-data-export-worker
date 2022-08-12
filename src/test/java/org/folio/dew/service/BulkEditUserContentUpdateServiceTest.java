package org.folio.dew.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.PREVIEW_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.PREVIEW_PREFIX;
import static org.folio.dew.utils.Constants.UPDATED_PREFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;
import org.folio.dew.domain.dto.UserContentUpdateCollection;
import org.folio.dew.service.update.BulkEditUserContentUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.UUID;

class BulkEditUserContentUpdateServiceTest extends BaseBatchTest {
  private static final String USER_DATA = "src/test/resources/upload/user_data.csv";

  @Autowired
  private BulkEditUserContentUpdateService contentUpdateService;

  @Test
  @SneakyThrows
  void shouldCreateUpdatedAndPreviewFilesAndUpdateJobCommand() {
    var uploadedFileName = FilenameUtils.getName(USER_DATA);
    var updatedFileName = UPDATED_PREFIX + uploadedFileName;
    var previewFileName = PREVIEW_PREFIX + uploadedFileName;
    minIOObjectStorageRepository.uploadObject(uploadedFileName, USER_DATA, null, "text/plain", false);
    var jobId = UUID.randomUUID();
    var jobCommand = new JobCommand();
    jobCommand.setId(jobId);
    jobCommand.setExportType(BULK_EDIT_IDENTIFIERS);
    jobCommand.setJobParameters(new JobParametersBuilder()
      .addString(TEMP_OUTPUT_FILE_PATH, "test/path/" + USER_DATA.replace(CSV_EXTENSION, EMPTY))
      .toJobParameters());

    jobCommandsReceiverService.addBulkEditJobCommand(jobCommand);

    var contentUpdates = new UserContentUpdateCollection()
      .userContentUpdates(Collections.singletonList(new UserContentUpdate()
        .option(UserContentUpdate.OptionEnum.PATRON_GROUP)
        .actions(Collections.singletonList(new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
          .value("new patron group")))))
      .totalRecords(1);

    var res = contentUpdateService.process(jobCommand, contentUpdates);

    assertThat(res.getUsersForPreview(), hasSize(2));
    assertThat(res.getUsersForPreview().stream().allMatch(userFormat -> "new patron group".equals(userFormat.getPatronGroup())), is(true));

    assertThat(minIOObjectStorageRepository.containsFile(updatedFileName), is(true));
    assertThat(minIOObjectStorageRepository.containsFile(previewFileName), is(true));

    assertThat(jobCommand.getExportType(), equalTo(BULK_EDIT_UPDATE));
    assertThat(jobCommand.getJobParameters().getString(UPDATED_FILE_NAME), equalTo(updatedFileName));
    assertThat(jobCommand.getJobParameters().getString(PREVIEW_FILE_NAME), equalTo(previewFileName));
  }
}
