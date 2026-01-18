package org.folio.dew.batch;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.Constants.NEW_LINE;
import static org.folio.dew.utils.WriterHelper.enrichHoldingsJson;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.dew.domain.dto.Formatable;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.utils.WriterHelper;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.core.io.WritableResource;

public class JsonFileWriter<T, U extends Formatable<T>> extends JsonFileItemWriter<U> {
  protected final JacksonJsonObjectMarshaller<T> marshaller;
  private final ObjectMapper objectMapper;
  public JsonFileWriter(WritableResource resource) {
    super(resource, new JacksonJsonObjectMarshaller<>());
    lineSeparator = EMPTY;
    setHeaderCallback(writer -> writer.write(EMPTY));
    setFooterCallback(writer -> writer.write(EMPTY));
    marshaller = new JacksonJsonObjectMarshaller<>();
    objectMapper = new ObjectMapper();
  }

  @SneakyThrows
  @Override
  public String doWrite(Chunk<? extends U> items) {
    var lines = new StringBuilder();
    var iterator = items.iterator();
    while (iterator.hasNext()) {
      var item = iterator.next();
      switch (item) {
        case HoldingsFormat hf -> lines.append(enrichHoldingsJson(hf, objectMapper));
        case InstanceFormat instanceFormat ->
            lines.append(WriterHelper.enrichInstancesJson(instanceFormat, objectMapper));
        case ItemFormat itemFormat ->
            lines.append(WriterHelper.enrichItemsJson(itemFormat, objectMapper));
        default -> lines.append(marshaller.marshal(item.getOriginal()));
      }
      lines.append(NEW_LINE);
    }
    return lines.toString();
  }
}
