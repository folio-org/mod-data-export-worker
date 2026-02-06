package org.folio.dew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.utils.MarcValidator.INVALID_MARC_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dew.client.SrsClient;
import org.folio.dew.error.MarcValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SrsServiceTest {
  private ObjectMapper objectMapper = new ObjectMapper();
  @Mock
  private SrsClient srsClient;
  @InjectMocks
  private SrsService srsService;
  @Spy
  private JsonToMarcConverter jsonToMarcConverter;

  @Test
  @SneakyThrows
  void shouldGetMarcJsonString() {
    var instanceId = UUID.randomUUID().toString();
    var srsJson = objectMapper.readTree(new File("src/test/resources/files/srs_response_for_validator.json"));

    when(srsClient.getMarc(instanceId, "INSTANCE", true)).thenReturn(srsJson);

    var result = srsService.getMarcJsonString(instanceId);

    var expected = "00079nam a2200037 a 4500008004100000\u001E170814m20172019nyua     6    000 1 eng d\u001E\u001D";
    assertThat(result).isEqualTo(expected);
  }

  @Test
  @SneakyThrows
  void shouldThrowMarcValidationExceptionIfMarcIsCorrupted() {
    var instanceId = UUID.randomUUID().toString();
    var srsJson = objectMapper.readTree(new File("src/test/resources/files/srs_response_corrupted_marc.json"));

    when(srsClient.getMarc(instanceId, "INSTANCE", true)).thenReturn(srsJson);

    var exception = assertThrows(MarcValidationException.class,
      () -> srsService.getMarcJsonString(instanceId));
    assertThat(exception.getMessage()).isEqualTo(INVALID_MARC_MESSAGE);
  }
}
