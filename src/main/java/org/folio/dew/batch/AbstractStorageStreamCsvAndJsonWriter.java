package org.folio.dew.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.folio.dew.domain.dto.Formatable;
import org.folio.dew.repository.S3CompatibleResource;
import org.folio.dew.repository.S3CompatibleStorage;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.core.io.WritableResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class AbstractStorageStreamCsvAndJsonWriter<O, T extends Formatable<O>, S extends S3CompatibleStorage> extends AbstractStorageStreamCsvWriter<T, S> {

  private JacksonJsonObjectMarshaller<O> jacksonJsonObjectMarshaller;

  private WritableResource jsonResource;

  public AbstractStorageStreamCsvAndJsonWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor, S storage) {
    super(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor, storage);
    setJsonResource(new S3CompatibleResource<>(tempOutputFilePath + "_json", storage));
    jacksonJsonObjectMarshaller = new JacksonJsonObjectMarshaller<>();
  }

  public void setJsonResource(S3CompatibleResource<S> jsonResource) {
    this.jsonResource = jsonResource;
  }

  @Override
  public void write(List<? extends T> items) throws Exception {
    var sb = new StringBuilder();
    var json = new StringBuilder();

    var iterator = items.iterator();
    while (iterator.hasNext()) {
      var item = iterator.next();
      sb.append(super.getLineAggregator().aggregate(item)).append('\n');

        json.append(jacksonJsonObjectMarshaller.marshal(item.getOriginal()));
        if(iterator.hasNext()) {
          json.append("," + '\n');
        }
    }
    getStorage().append(getResource().getFilename(), sb.toString().getBytes(StandardCharsets.UTF_8));
    getStorage().append(jsonResource.getFilename(), json.toString().getBytes(StandardCharsets.UTF_8));
  }
}
