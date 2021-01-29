package org.folio.model;

import org.folio.dto.GreetingDto;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ItemWriterFactory {

    public FlatFileItemWriter<GreetingDto> getFlatFileItemWriter(String outputFilePath) {
        final String commaDelimiter = ",";

        // TODO validate input parameters there

        Resource outputFile = new FileSystemResource(outputFilePath);

        FlatFileItemWriter<GreetingDto> flatFileItemWriter = new FlatFileItemWriterBuilder<GreetingDto>()
                .name("greetingsWriter")
                .resource(outputFile)
                .delimited()
                .delimiter(commaDelimiter)
                .names(new String[] { "id", "greeting", "language"})
                .build();

        return flatFileItemWriter;
    }
}
