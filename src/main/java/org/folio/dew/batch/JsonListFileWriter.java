package org.folio.dew.batch;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.dew.domain.dto.Formatable;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.utils.WriterHelper;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.core.io.WritableResource;

import java.util.List;

public class JsonListFileWriter<T, U extends Formatable<T>> extends JsonFileItemWriter<List<U>> {
  private final JacksonJsonObjectMarshaller<T> marshaller;
  private final ObjectMapper objectMapper;
  public JsonListFileWriter(WritableResource resource) {
    super(resource, new JacksonJsonObjectMarshaller<>());
    lineSeparator = EMPTY;
    setHeaderCallback(writer -> writer.write(EMPTY));
    setFooterCallback(writer -> writer.write(EMPTY));
    marshaller = new JacksonJsonObjectMarshaller<>();
    objectMapper = new ObjectMapper();
  }

  @Override
  public String doWrite(Chunk<? extends List<U>> lists) {
    var lines = new StringBuilder();
    var chunk = new Chunk<>(lists.getItems().stream().flatMap(List::stream).toList());
    var iterator = chunk.iterator();
    while (iterator.hasNext()) {
      var item = iterator.next();
      if (item instanceof HoldingsFormat hf) {
        lines.append(WriterHelper.enrichHoldingsJson(hf, objectMapper));
      } else {
        lines.append(marshaller.marshal(item.getOriginal()));
      }
      if(iterator.hasNext()) {
        lines.append('\n');
      }
    }
    return lines.toString();
  }
}
