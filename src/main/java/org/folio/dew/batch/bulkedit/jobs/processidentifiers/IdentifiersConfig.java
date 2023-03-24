package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.utils.Constants.TEMP_IDENTIFIERS_FILE_NAME;

import org.folio.dew.domain.dto.ItemIdentifier;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class IdentifiersConfig {
  @Bean
  @StepScope
  public FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader(
    @Value("#{jobParameters['" + TEMP_IDENTIFIERS_FILE_NAME + "']}") String uploadedFileName) {
    return new FlatFileItemReaderBuilder<ItemIdentifier>()
      .name("userItemIdentifierReader")
      .resource(new FileSystemResource(uploadedFileName))
      .linesToSkip(0)
      .lineMapper(lineMapper())
      .build();
  }

  @Bean
  public LineMapper<ItemIdentifier> lineMapper() {
    var lineMapper = new DefaultLineMapper<ItemIdentifier>();
    var tokenizer = new DelimitedLineTokenizer();
    tokenizer.setNames("itemId");
    var fieldSetMapper = new BeanWrapperFieldSetMapper<ItemIdentifier>();
    fieldSetMapper.setTargetType(ItemIdentifier.class);
    lineMapper.setLineTokenizer(tokenizer);
    lineMapper.setFieldSetMapper(fieldSetMapper);
    return lineMapper;
  }
}
