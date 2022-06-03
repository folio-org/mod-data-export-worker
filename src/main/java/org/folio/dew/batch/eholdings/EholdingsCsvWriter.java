package org.folio.dew.batch.eholdings;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
import static org.folio.dew.utils.Constants.LINE_BREAK;
import static org.folio.dew.utils.Constants.LINE_BREAK_REPLACEMENT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.springframework.batch.item.support.AbstractFileItemWriter;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.ClassUtils;

public class EholdingsCsvWriter extends AbstractFileItemWriter<EHoldingsResourceExportFormat> {

  private static final String PACKAGE_NOTES_FIELD = "packageNotes";
  private static final String TITLE_NOTES_FIELD = "titleNotes";
  private final String[] fieldNames;

  public EholdingsCsvWriter(String tempOutputFilePath, String[] fieldNames) {
    this.fieldNames = fieldNames;
    if (isBlank(tempOutputFilePath)) {
      throw new IllegalArgumentException("tempOutputFilePath is blank");
    }
    setResource(new FileSystemResource(tempOutputFilePath));
    this.setExecutionContextName(ClassUtils.getShortName(EholdingsCsvWriter.class));
  }

  @Override
  public void afterPropertiesSet() {
    if (append) {
      shouldDeleteIfExists = false;
    }
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
    BeanWrapper bw = new BeanWrapperImpl(item);
    for (String fieldName : fieldNames) {
      var value = bw.getPropertyValue(fieldName);
      if (value instanceof String) {
        String s = getStringValue((String) value);
        itemValues.add(s);
      } else if (value instanceof List) {
        @SuppressWarnings("unchecked") var strings = (List<String>) value;
        getListValue(maxPackageNotesLength, maxTitleNotesLength, itemValues, fieldName, strings);
      } else {
        itemValues.add(EMPTY);
      }
    }
    return String.join(",", itemValues);
  }

  private void getListValue(Integer maxPackageNotesLength, Integer maxTitleNotesLength, ArrayList<String> itemValues,
                            String fieldName, List<String> value) {
    for (String s : value) {
      itemValues.add(cleanupValue(s));
    }
    if (fieldName.equals(PACKAGE_NOTES_FIELD) && value.size() < maxPackageNotesLength) {
      for (int i = 0; i < maxPackageNotesLength - value.size(); i++) {
        itemValues.add(EMPTY);
      }
    } else if (fieldName.equals(TITLE_NOTES_FIELD) && value.size() < maxTitleNotesLength) {
      for (int i = 0; i < maxTitleNotesLength - value.size(); i++) {
        itemValues.add(EMPTY);
      }
    }
  }

  private String cleanupValue(String s) {
    if (s.contains(",") || s.contains("\n")) {
      s = "\"" + s.replace("\"", "\"\"").replace(LINE_BREAK, LINE_BREAK_REPLACEMENT) + "\"";
    }
    return s;
  }

  private String getStringValue(String value) {
    return cleanupValue(value);
  }

  @Override
  protected String doWrite(List<? extends EHoldingsResourceExportFormat> items) {
    var maxPackageNotesLength = items.stream().map(e -> e.getPackageNotes().size()).max(Integer::compareTo).orElse(0);
    var maxTitleNotesLength = items.stream().map(e -> e.getTitleNotes().size()).max(Integer::compareTo).orElse(0);

    StringBuilder lines = new StringBuilder();

    String columnHeaders =
      Arrays.stream(fieldNames).map(s -> header(s, maxPackageNotesLength, maxTitleNotesLength)).flatMap(List::stream)
        .collect(Collectors.joining(","));

    lines.append(columnHeaders).append(lineSeparator);

    for (var item : items) {
      var itemRow = getItemRow(maxPackageNotesLength, maxTitleNotesLength, item);
      lines.append(itemRow).append(lineSeparator);
    }

    return lines.toString();
  }
}
