package org.folio.dew.batch.eholdings;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_MAX_PACKAGE_NOTES_COUNT;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_MAX_TITLE_NOTES_COUNT;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.LOAD_FIELD_PACKAGE_NOTES;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.LOAD_FIELD_TITLE_NOTES;
import static org.folio.dew.utils.Constants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceExportFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
@StepScope
public class EHoldingsCsvFileWriter extends AbstractFileItemWriter<EHoldingsResourceExportFormat> {
  private final String[] fieldNames;
  private int maxPackageNotesLength;
  private int maxTitleNotesLength;
  private final String tempOutputFilePath;
  @Autowired
  private LocalFilesStorage localFilesStorage;

  public EHoldingsCsvFileWriter(@Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
                                EHoldingsExportConfig exportConfig) {
    this.fieldNames = getFieldNames(exportConfig);
    setEholdingsResource(tempOutputFilePath);
    this.setExecutionContextName(ClassUtils.getShortName(EHoldingsCsvFileWriter.class));
    this.tempOutputFilePath = tempOutputFilePath;
  }

  private void setEholdingsResource(String tempOutputFilePath) {
    if (isBlank(tempOutputFilePath)) {
      throw new IllegalArgumentException("tempOutputFilePath is blank");
    }
    var resource = new S3CompatibleResource<>(tempOutputFilePath, localFilesStorage);
    setResource(resource);
  }

  private String[] getFieldNames(EHoldingsExportConfig exportConfig) {
    var exportFields = new ArrayList<String>();
    if (exportConfig.getPackageFields() != null) {
      exportFields.addAll(exportConfig.getPackageFields());
    }
    if (exportConfig.getTitleFields() != null) {
      exportFields.addAll(exportConfig.getTitleFields());
    }
    if (exportFields.isEmpty()) {
      throw new IllegalArgumentException("Export fields are empty");
    }

    return exportFields.toArray(String[]::new);
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) throws IOException {
    var executionContext = stepExecution.getJobExecution().getExecutionContext();
    maxPackageNotesLength = executionContext.getInt(CONTEXT_MAX_PACKAGE_NOTES_COUNT, 0);
    maxTitleNotesLength = executionContext.getInt(CONTEXT_MAX_TITLE_NOTES_COUNT, 0);

    String columnHeaders = Arrays.stream(fieldNames)
      .map(s -> header(s, maxPackageNotesLength, maxTitleNotesLength))
      .flatMap(List::stream)
      .collect(Collectors.joining(","));

    localFilesStorage.write(tempOutputFilePath, (columnHeaders + lineSeparator).getBytes());
  }

  @Override
  public void afterPropertiesSet() {
    if (append) {
      shouldDeleteIfExists = false;
    }
  }

  @NotNull
  @Override
  protected String doWrite(List<? extends EHoldingsResourceExportFormat> items) {
    var lines = new StringBuilder();

    for (var item : items) {
      var itemRow = getItemRow(maxPackageNotesLength, maxTitleNotesLength, item);
      lines.append(itemRow).append(lineSeparator);
    }

    return lines.toString();
  }

  @Override
  public void write(List<? extends EHoldingsResourceExportFormat> items) throws Exception {
    localFilesStorage.append(tempOutputFilePath, (doWrite(items)).getBytes());
  }


  private List<String> header(String fieldName, int maxPackageNotesLength, int maxTitleNotesLength) {
    if (fieldName.equals(LOAD_FIELD_PACKAGE_NOTES)) {
      return getHeadersList(maxPackageNotesLength, "Package Note");
    } else if (fieldName.equals(LOAD_FIELD_TITLE_NOTES)) {
      return getHeadersList(maxTitleNotesLength, "Title Note");
    } else {
      return List.of(capitalize(join(splitByCharacterTypeCamelCase(fieldName), SPACE)));
    }
  }

  private List<String> getHeadersList(int length, String name) {
    if (length == 0) {
      return Collections.singletonList(name);
    }
    return IntStream.range(1, length + 1).boxed().map(i -> name + " " + i).collect(Collectors.toList());
  }

  private String getItemRow(int maxPackageNotesLength, int maxTitleNotesLength, EHoldingsResourceExportFormat item) {
    var itemValues = new ArrayList<String>();
    var bw = new BeanWrapperImpl(item);
    for (var fieldName : fieldNames) {
      var value = bw.getPropertyValue(fieldName);
      if (value instanceof String) {
        var s = getStringValue((String) value);
        itemValues.add(s);
      } else if (value instanceof List) {
        @SuppressWarnings("unchecked") var strings = (List<String>) value;
        itemValues.addAll(getListValue(maxPackageNotesLength, maxTitleNotesLength, fieldName, strings));
      } else {
        itemValues.add(EMPTY);
      }
    }
    return String.join(",", itemValues);
  }

  private List<String> getListValue(int maxPackageNotesLength, int maxTitleNotesLength, String fieldName,
                                    List<String> value) {
    var strings = new ArrayList<String>();
    for (var s : value) {
      strings.add(quoteValue(s));
    }
    if (fieldName.equals(LOAD_FIELD_PACKAGE_NOTES)) {
      fillWithBlanks(strings, value.size(), maxPackageNotesLength);
    } else if (fieldName.equals(LOAD_FIELD_TITLE_NOTES)) {
      fillWithBlanks(strings, value.size(), maxTitleNotesLength);
    }
    return strings;
  }

  private void fillWithBlanks(List<String> strings, int valuesSize, int maxValuesSize) {
    if (valuesSize < maxValuesSize) {
      for (var i = 0; i < maxValuesSize - valuesSize; i++) {
        strings.add(EMPTY);
      }
    } else if (maxValuesSize == 0) {
      strings.add(EMPTY);
    }

  }

  private String quoteValue(String s) {
    if (s.contains(COMMA) || s.contains(LINE_BREAK)) {
      s = QUOTE + s.replace(QUOTE, QUOTE_REPLACEMENT).replace(LINE_BREAK, LINE_BREAK_REPLACEMENT) + QUOTE;
    }
    return s;
  }

  private String getStringValue(String value) {
    return quoteValue(value);
  }
}
