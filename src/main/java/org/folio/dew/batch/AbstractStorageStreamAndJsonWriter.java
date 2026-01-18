package org.folio.dew.batch;

import static java.lang.System.lineSeparator;
import static org.folio.dew.utils.WriterHelper.enrichHoldingsJson;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.folio.dew.domain.dto.Formatable;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.repository.S3CompatibleResource;
import org.folio.dew.repository.S3CompatibleStorage;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.core.io.WritableResource;

import java.nio.charset.StandardCharsets;

@Slf4j
public class AbstractStorageStreamAndJsonWriter<O, T extends Formatable<O>, S extends S3CompatibleStorage>
    extends AbstractStorageStreamWriter<T, S> implements StepExecutionListener {

  private final JacksonJsonObjectMarshaller<O> jacksonJsonObjectMarshaller;
  private final ObjectMapper objectMapper;

  private WritableResource jsonResource;

  private Path csvTmpFile;
  private Path jsonTmpFile;

  public AbstractStorageStreamAndJsonWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor, S storage) {
    super(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor, storage);
    setJsonResource(new S3CompatibleResource<>(tempOutputFilePath + ".json", storage));
    jacksonJsonObjectMarshaller = new JacksonJsonObjectMarshaller<>();
    objectMapper = new ObjectMapper();
  }

  public void setJsonResource(S3CompatibleResource<S> jsonResource) {
    this.jsonResource = jsonResource;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    try {
      Path tmpDir = createPrivateTmpDir();
      csvTmpFile = Files.createTempFile(tmpDir, "dew-csv-", ".tmp");
      jsonTmpFile = Files.createTempFile(tmpDir, "dew-json-", ".tmp");
    } catch (IOException e) {
      throw new IllegalStateException("Error creating tmp files for resources", e);
    }
  }

  private Path createPrivateTmpDir() throws IOException {
    Path parent = Path.of(System.getProperty("java.io.tmpdir"));
    FileAttribute<?>[] attrs = new FileAttribute[0];

    FileSystem fs = FileSystems.getDefault();
    if (fs.supportedFileAttributeViews().contains("posix")) {
      attrs = new FileAttribute[]{
          PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))
      };
    }
    return Files.createTempDirectory(parent, "dew-", attrs);
  }


  @Override
  public void write(Chunk<? extends T> items) throws Exception {

    if (items.isEmpty()) {
      return;
    }

    boolean jsonHasDataAlready = Files.size(jsonTmpFile) > 0;

    try (BufferedWriter plainWriter = Files.newBufferedWriter(
            csvTmpFile,
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND);
        BufferedWriter jsonWriter = Files.newBufferedWriter(
            jsonTmpFile,
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND)) {

      if (jsonHasDataAlready) {
        jsonWriter.append(lineSeparator());
      }

      var iterator = items.iterator();
      while (iterator.hasNext()) {
        var item = iterator.next();

        plainWriter.append(super.getLineAggregator().aggregate(item))
            .append(lineSeparator());

        if (item instanceof HoldingsFormat hf) {
          jsonWriter.append(enrichHoldingsJson(hf, objectMapper));
        } else {
          jsonWriter.append(jacksonJsonObjectMarshaller.marshal(item.getOriginal()));
        }

        if (iterator.hasNext()) {
          jsonWriter.append(lineSeparator());
        }
      }
    }
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    try {
      getStorage().write(getResource().getFilename(),
          ArrayUtils.addAll(getStorage().readAllBytes(getResource().getFilename()), Files.readAllBytes(csvTmpFile)));
      getStorage().write(jsonResource.getFilename(), Files.readAllBytes(jsonTmpFile));
    } catch (IOException e) {
      log.error("Error uploading data to S3", e);
      return ExitStatus.FAILED.addExitDescription(e.getMessage());
    } finally {
      deleteTempFile(csvTmpFile);
      deleteTempFile(jsonTmpFile);
    }
    return stepExecution.getExitStatus();
  }

  private void deleteTempFile(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException ex) {
      log.warn("Error deleting tmp-file {}", path, ex);
    }
  }
}
