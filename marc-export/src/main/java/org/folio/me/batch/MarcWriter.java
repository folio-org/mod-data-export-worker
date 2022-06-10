package org.folio.me.batch;

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
      for (Record r : records) {
        marcStreamWriter.write(r);
      }
      return byteArrayOutputStream.toString();
    } catch (IOException e) {
      log.error("Error writing marc record, reason: {}", e.getMessage());
      return null;
    }
  }

  @Override
  public void afterPropertiesSet() {
    // no mandatory properties required
  }
}
