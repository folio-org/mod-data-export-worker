package org.folio.dew.service;

import static org.folio.dew.utils.Constants.MULTIPLE_SRS;
import static org.folio.dew.utils.Constants.SRS_MISSING;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.SrsClient;
import org.folio.dew.error.MarcValidationException;
import org.folio.dew.utils.MarcValidator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class SrsService {
  private final SrsClient srsClient;
  private final JsonToMarcConverter jsonToMarcConverter;

  public String getMarcJsonString(String instanceId) throws MarcValidationException, IOException {
    var srsRecords = srsClient.getMarc(instanceId, "INSTANCE", true).get("sourceRecords");
    if (srsRecords.isEmpty()) {
      throw new MarcValidationException(SRS_MISSING);
    } else if (srsRecords.size() > 1) {
      throw new MarcValidationException(
          MULTIPLE_SRS.formatted(String.join(", ", getAllSrsIds(srsRecords))));
    } else {
      var srsRec = srsRecords.elements().next();
      var parsedRec = srsRec.get("parsedRecord");
      var marcJsonString = parsedRec.get("content").toString();
      MarcValidator.validate(marcJsonString);
      return jsonToMarcConverter.convertJsonRecordToMarcRecord(marcJsonString);
    }
  }

  private String getAllSrsIds(JsonNode srsRecords) {
    return String.join(
        ", ",
        StreamSupport.stream(srsRecords.spliterator(), false)
            .map(n -> StringUtils.strip(n.get("recordId").toString(), "\""))
            .toList());
  }
}
