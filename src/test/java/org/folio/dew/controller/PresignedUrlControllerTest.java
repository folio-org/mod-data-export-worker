package org.folio.dew.controller;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.repository.RemoteFilesStorage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyString;

public class PresignedUrlControllerTest extends BaseBatchTest {
  @MockBean
  private RemoteFilesStorage filesStorage;

  private static final String REFRESH_PRESIGNED_URL = "/refresh-presigned-url";
  private static final String FILE_PATH = "filePath";
  private static final String FILE_PATH_EXAMPLE = "file/path/example";
  private static final String PRESIGNED_URL_EXAMPLE = "http://test/url";

  @Test
  void shouldRetrievePresignedUrl() throws Exception {
    Mockito.when(filesStorage.objectToPresignedObjectUrl(anyString())).thenReturn(PRESIGNED_URL_EXAMPLE);

    var headers = defaultHeaders();

    mockMvc.perform(get(REFRESH_PRESIGNED_URL)
        .headers(headers)
        .queryParam(FILE_PATH, FILE_PATH_EXAMPLE))
      .andExpect(status().isOk());
  }
}
