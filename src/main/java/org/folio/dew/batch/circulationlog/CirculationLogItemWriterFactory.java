package org.folio.dew.batch.circulationlog;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.LogRecord;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class CirculationLogItemWriterFactory {

  public FlatFileItemWriter<LogRecord> getFlatFileItemWriter(String outputFilePath) {
    Resource outputFile = new FileSystemResource(outputFilePath);
    log.info("Creating file {}.", outputFilePath);
    return new FlatFileItemWriterBuilder<LogRecord>().name("circulationLogWriter")
        .resource(outputFile)
        .delimited()
        .delimiter(",")
        .names("id", "eventId", "userBarcode", "items", "object", "action", "date", "servicePointId", "source", "description",
            "linkToIds")
        .build();
  }

}
