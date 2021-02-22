package org.folio.dew.model.circulationlog;

import org.folio.dew.domain.dto.LogRecord;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class CirculationLogItemWriterFactory {

  public FlatFileItemWriter<LogRecord> getFlatFileItemWriter(String outputFilePath) {
    final String commaDelimiter = ",";

    Resource outputFile = new FileSystemResource(outputFilePath);

    return new FlatFileItemWriterBuilder<LogRecord>().name("circulationLogWriter")
        .resource(outputFile)
        .delimited()
        .delimiter(commaDelimiter)
        .names("id", "eventId", "userBarcode", "items", "object", "action", "date", "servicePointId", "source", "description",
            "linkToIds")
        .build();
  }

}
