package org.folio.dew.batch.marc;

import lombok.extern.log4j.Log4j2;
import org.marc4j.MarcJsonWriter;
import org.marc4j.marc.Record;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Log4j2
public class MarcWriter extends FlatFileItemWriter<Record> {

  public MarcWriter(String tempOutputFilePath) {
    setAppendAllowed(true);
    setResource(new FileSystemResource(tempOutputFilePath));
    log.info("Creating file {}.", tempOutputFilePath);
  }

  @Override
  public String doWrite(List<? extends Record> records) {
    try (var byteArrayOutputStream = new ByteArrayOutputStream()) {
      var marcStreamWriter = new MarcJsonWriter(byteArrayOutputStream);
      for (Record record : records) {
        marcStreamWriter.write(record);
      }
      return byteArrayOutputStream.toString();
    } catch (IOException e) {
      // TODO log file operation error
      return null;
    }
  }

  @Override
  public void afterPropertiesSet() {
    // no mandatory properties required
  }
}
