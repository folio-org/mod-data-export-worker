package org.folio.dew.batch.acquisitions.services;

import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.ExportHistory;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.PoLine;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ExportHistoryService {
  private final KafkaService kafkaService;

  void sendExportHistoryEvent(Set<PoLine> poLines, String jobId) {
    var exportHistory = buildExportHistoryRecord(poLines, jobId);
    var id = UUID.randomUUID().toString();

    kafkaService.send(KafkaService.Topic.EXPORT_HISTORY_CREATE, id, exportHistory);
  }

  private ExportHistory buildExportHistoryRecord(Set<PoLine> poLines, String jobId) {
    ExportHistory eh = new ExportHistory();
    var poLineIds = poLines.stream().map(PoLine::getId).collect(Collectors.toList());
    eh.id(UUID.randomUUID().toString());
    // TODO: check TIME ZONE
    eh.exportDate(Date.from(Instant.now()));
    eh.exportType(ExportType.EDIFACT_ORDERS_EXPORT.getValue());
    eh.exportedPoLineIds(poLineIds);
    eh.exportJobId(jobId);
    log.info("Prepared Export History record: {}", eh);
    return eh;
  }

}
