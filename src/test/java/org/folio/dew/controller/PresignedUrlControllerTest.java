package org.folio.dew.controller;

import org.apache.commons.io.FilenameUtils;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.repository.RemoteFilesStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

public class PresignedUrlControllerTest extends BaseBatchTest {
  @Autowired
  private RemoteFilesStorage remoteFilesStorage;

  private static final String PREVIEW_ITEM_DATA = "src/test/resources/upload/preview_item_data.csv";
  private static final String REFRESH_PRESIGNED_URL = "/refresh-presigned-url";
  private static final String FILE_PATH = "filePath";

  @Test
  void shouldRetrievePresignedUrl() throws Exception {
    var jobId = UUID.randomUUID();
    var filePath = jobId + PATH_SEPARATOR + FilenameUtils.getName(PREVIEW_ITEM_DATA);
    remoteFilesStorage.upload(filePath, PREVIEW_ITEM_DATA);

    var headers = defaultHeaders();

    mockMvc.perform(get(REFRESH_PRESIGNED_URL)
        .headers(headers)
        .queryParam(FILE_PATH, filePath))
      .andExpect(status().isOk());
  }
}
