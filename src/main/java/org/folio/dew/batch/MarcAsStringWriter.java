package org.folio.dew.batch;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
@StepScope
public class MarcAsStringWriter extends FlatFileItemWriter<List<String>> implements ItemWriter<List<String>> {

  public MarcAsStringWriter(String outputFileName) {
    super();
    setResource(new FileSystemResource(outputFileName + ".mrc"));
    setLineSeparator(EMPTY);
    setLineAggregator(new MrcFileLineAggregator());
    setShouldDeleteIfEmpty(true);
    setName("marcItemWriter");
  }

  @Override
  public void write(Chunk<? extends List<String>> chunk) throws Exception {
    if (chunk.getItems().stream().anyMatch(str -> !str.isEmpty())) {
      super.write(chunk);
    }
  }
}
