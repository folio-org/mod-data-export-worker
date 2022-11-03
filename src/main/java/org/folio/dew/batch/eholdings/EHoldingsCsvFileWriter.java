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
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;
import static org.folio.dew.utils.Constants.COMMA;
import static org.folio.dew.utils.Constants.LINE_BREAK;
import static org.folio.dew.utils.Constants.LINE_BREAK_REPLACEMENT;
import static org.folio.dew.utils.Constants.QUOTE;
import static org.folio.dew.utils.Constants.QUOTE_REPLACEMENT;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.folio.de.entity.EHoldingsPackage;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceExportFormat;
import org.folio.dew.repository.EHoldingsPackageRepository;
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
  private int maxPackageNotesLength;
  private int maxTitleNotesLength;
  private final String tempOutputFilePath;
  @Autowired
  private LocalFilesStorage localFilesStorage;
  private final EHoldingsPackageRepository packageRepository;
  private final EHoldingsExportConfig exportConfig;
  private final EHoldingsToExportFormatMapper mapper;

  public EHoldingsCsvFileWriter(@Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
                                EHoldingsExportConfig exportConfig,
                                EHoldingsPackageRepository packageRepository,
                                EHoldingsToExportFormatMapper mapper) {
    setEholdingsResource(tempOutputFilePath);
    this.setExecutionContextName(ClassUtils.getShortName(EHoldingsCsvFileWriter.class));
    this.tempOutputFilePath = tempOutputFilePath;
    this.packageRepository = packageRepository;
    this.exportConfig = exportConfig;
    this.mapper = mapper;
  }

  private void setEholdingsResource(String tempOutputFilePath) {
    if (isBlank(tempOutputFilePath)) {
      throw new IllegalArgumentException("tempOutputFilePath is blank");
    }
    setResource(new S3CompatibleResource<>(tempOutputFilePath, localFilesStorage));
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) throws IOException {
    var executionContext = stepExecution.getJobExecution().getExecutionContext();
    maxPackageNotesLength = executionContext.getInt(CONTEXT_MAX_PACKAGE_NOTES_COUNT, 0);
    maxTitleNotesLength = executionContext.getInt(CONTEXT_MAX_TITLE_NOTES_COUNT, 0);

    writePackage(stepExecution.getJobExecutionId());

    var resourceHeaders = getHeader(exportConfig.getTitleFields()) + lineSeparator;
    writeString(resourceHeaders);
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
    return items.stream()
      .map(item -> getItemRow(maxTitleNotesLength, item, exportConfig.getTitleFields()))
      .collect(Collectors.joining(lineSeparator));
  }

  @Override
  public void write(List<? extends EHoldingsResourceExportFormat> items) throws Exception {
    writeString(doWrite(items));
  }

  private void writePackage(Long jobExecutionId) throws IOException {

    var packageFields = exportConfig.getPackageFields();
    var packageHeader = getHeader(packageFields) + lineSeparator;
    writeString(packageHeader);

    var recordId = exportConfig.getRecordId();
    var packageId = exportConfig.getRecordType() == PACKAGE ? recordId : recordId.split("-\\d+$")[0];
    var packageComposedId = new EHoldingsPackage.PackageId();
    packageComposedId.setId(packageId);
    packageComposedId.setJobExecutionId(jobExecutionId);
    var eHoldingsPackage = packageRepository.findById(packageComposedId).orElse(null);
    var packageExportFormat = mapper.convertToExportFormat(eHoldingsPackage);

    var packageRow = getItemRow(maxPackageNotesLength, packageExportFormat, exportConfig.getPackageFields()) + lineSeparator;
    writeString(packageRow);
  }

  private void writeString(String str) throws IOException {
    localFilesStorage.append(tempOutputFilePath, str.getBytes(StandardCharsets.UTF_8));
  }

  private String getHeader(List<String> fieldNames) {
    return fieldNames.stream()
      .map(this::headerColumns)
      .flatMap(List::stream)
      .collect(Collectors.joining(","));
  }

  private List<String> headerColumns(String fieldName) {
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

  private String getItemRow(int maxFieldColumnsCount, Object item, List<String> exportFieldNames) {
    var itemValues = new ArrayList<String>();
    var bw = new BeanWrapperImpl(item);
    for (var fieldName : exportFieldNames) {
      var value = bw.getPropertyValue(fieldName);
      if (value instanceof String) {
        var s = getStringValue((String) value);
        itemValues.add(s);
      } else if (value instanceof List) {
        @SuppressWarnings("unchecked") var strings = (List<String>) value;
        itemValues.addAll(getListValue(maxFieldColumnsCount, fieldName, strings));
      } else {
        itemValues.add(EMPTY);
      }
    }
    return String.join(",", itemValues);
  }

  private List<String> getListValue(int maxFieldColumnsCount, String fieldName,
                                    List<String> value) {
    var strings = new ArrayList<String>();
    for (var s : value) {
      strings.add(quoteValue(s));
    }
    if (fieldName.equals(LOAD_FIELD_PACKAGE_NOTES) || fieldName.equals(LOAD_FIELD_TITLE_NOTES)) {
      fillWithBlanks(strings, value.size(), maxFieldColumnsCount);
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
