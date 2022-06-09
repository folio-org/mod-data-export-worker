package org.folio.dew.batch.eholdings;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
import static org.folio.dew.utils.Constants.COMMA;
import static org.folio.dew.utils.Constants.LINE_BREAK;
import static org.folio.dew.utils.Constants.LINE_BREAK_REPLACEMENT;
import static org.folio.dew.utils.Constants.QUOTE;
import static org.folio.dew.utils.Constants.QUOTE_REPLACEMENT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.ClassUtils;

public class EholdingsCsvWriter extends AbstractFileItemWriter<EHoldingsResourceExportFormat> {

  private static final String PACKAGE_NOTES_FIELD = "packageNotes";
  private static final String TITLE_NOTES_FIELD = "titleNotes";
  private final String[] fieldNames;
  private int maxPackageNotesLength;
  private int maxTitleNotesLength;

  public EholdingsCsvWriter(String tempOutputFilePath, String[] fieldNames) {
    this.fieldNames = fieldNames;
    if (isBlank(tempOutputFilePath)) {
      throw new IllegalArgumentException("tempOutputFilePath is blank");
    }
    setResource(new FileSystemResource(tempOutputFilePath));
    this.setExecutionContextName(ClassUtils.getShortName(EholdingsCsvWriter.class));
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    var executionContext = stepExecution.getJobExecution().getExecutionContext();
    maxPackageNotesLength = executionContext.getInt("packageMaxNotesCount", 0);
    maxTitleNotesLength = executionContext.getInt("titleMaxNotesCount", 0);

    var columnHeaders = Arrays.stream(fieldNames)
      .map(s -> header(s, maxPackageNotesLength, maxTitleNotesLength))
      .flatMap(List::stream)
      .collect(Collectors.joining(","));
    setHeaderCallback(writer -> writer.write(columnHeaders));
  }

  @Override
  public void afterPropertiesSet() {
    if (append) {
      shouldDeleteIfExists = false;
    }
  }

  @Override
  protected String doWrite(List<? extends EHoldingsResourceExportFormat> items) {
    var lines = new StringBuilder();

    for (var item : items) {
      var itemRow = getItemRow(maxPackageNotesLength, maxTitleNotesLength, item);
      lines.append(itemRow).append(lineSeparator);
    }

    return lines.toString();
  }

  private List<String> header(String fieldName, int maxPackageNotesLength, int maxTitleNotesLength) {
    if (fieldName.equals(PACKAGE_NOTES_FIELD)) {
      return getHeadersList(maxPackageNotesLength, "Package Note");
    } else if (fieldName.equals(TITLE_NOTES_FIELD)) {
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

  private String getItemRow(Integer maxPackageNotesLength, Integer maxTitleNotesLength,
                            EHoldingsResourceExportFormat item) {
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

  private List<String> getListValue(Integer maxPackageNotesLength, Integer maxTitleNotesLength,
                                    String fieldName, List<String> value) {
    var strings = new ArrayList<String>();
    for (var s : value) {
      strings.add(cleanupValue(s));
    }
    if (fieldName.equals(PACKAGE_NOTES_FIELD) && value.size() < maxPackageNotesLength) {
      fillWithBlanks(strings, maxPackageNotesLength - value.size());
    } else if (fieldName.equals(TITLE_NOTES_FIELD) && value.size() < maxTitleNotesLength) {
      fillWithBlanks(strings, maxTitleNotesLength - value.size());
    }
    return strings;
  }

  private void fillWithBlanks(ArrayList<String> strings, int blankCount) {
    for (var i = 0; i < blankCount; i++) {
      strings.add(EMPTY);
    }
  }

  private String cleanupValue(String s) {
    if (s.contains(COMMA) || s.contains(LINE_BREAK)) {
      s = QUOTE + s.replace(QUOTE, QUOTE_REPLACEMENT).replace(LINE_BREAK, LINE_BREAK_REPLACEMENT) + QUOTE;
    }
    return s;
  }

  private String getStringValue(String value) {
    return cleanupValue(value);
  }
}
