package org.folio.dew.batch.marc;

import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.FileOperationException;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class DataExportCsvItemReader extends CsvItemReader<ItemIdentifier> {

  private static final int QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST = 100;

  private final String fileName;

  public DataExportCsvItemReader(String fileName, Long offset, Long limit) {
    super(offset, limit, QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST);

    this.fileName = fileName;
  }

  @Override
  protected List<ItemIdentifier> getItems(int offset, int limit) {
    try {
      try (var lines = Files.lines(Path.of(new URI(fileName)))) {
        return lines
          .skip(offset)
          .limit(limit)
          .map(ItemIdentifier::new)
          .collect(Collectors.toList());
      }
    } catch (Exception e) {
      throw new FileOperationException(e.getMessage());
    }
  }

}
