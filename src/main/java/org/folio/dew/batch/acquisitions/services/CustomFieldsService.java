package org.folio.dew.batch.acquisitions.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.acquisitions.customfields.CustomField;
import org.folio.dew.domain.dto.acquisitions.customfields.SelectField;
import org.folio.dew.domain.dto.acquisitions.customfields.SelectFieldOption;
import org.folio.dew.domain.dto.acquisitions.customfields.SelectFieldOptions;
import org.folio.dew.domain.dto.templateengine.context.CustomFieldContext;
import org.folio.dew.domain.dto.templateengine.context.CustomFieldOptionValue;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Turns the raw {@code refId -> value} custom-fields map carried on a purchase order or PO line
 * into template-ready {@link CustomFieldContext} entries, resolving select option-ids to labels.
 *
 * <p>Value shape:
 * <ul>
 *   <li>single-select scalar → {@code value} = {@link CustomFieldOptionValue}{@code {id, label}}</li>
 *   <li>multi-select / repeatable select array → {@code values[]} of {@link CustomFieldOptionValue}</li>
 *   <li>checkbox scalar → {@code value} = {@code Boolean}</li>
 *   <li>textbox/date/number scalar → {@code value} = {@code String}</li>
 *   <li>repeatable text array → {@code values[]} of plain {@code String}</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CustomFieldsService {

  private static final String TYPE_SINGLE_CHECKBOX = "SINGLE_CHECKBOX";

  private final CustomFieldDefinitionService definitionService;

  public Map<String, CustomFieldContext> resolve(Map<String, Object> raw, String entityType) {
    if (MapUtils.isEmpty(raw)) {
      return Map.of();
    }
    var definitions = definitionService.getDefinitionsByRefId(entityType);
    Map<String, CustomFieldContext> result = new LinkedHashMap<>();
    raw.forEach((refId, rawValue) -> {
      var definition = definitions.get(refId);
      if (rawValue == null || definition == null || Boolean.FALSE.equals(definition.getVisible())) {
        return;
      }
      var builder = CustomFieldContext.builder()
        .name(definition.getName())
        .type(definition.getType());
      if (rawValue instanceof List<?> list) {
        builder.values(list.stream()
          .filter(Objects::nonNull)
          .map(element -> toElement(definition, element))
          .toList());
      } else {
        builder.value(toScalar(definition, rawValue));
      }
      result.put(refId, builder.build());
    });
    return Collections.unmodifiableMap(result);
  }

  private Object toScalar(CustomField definition, Object rawValue) {
    if (isSelect(definition)) {
      return toOptionValue(definition, String.valueOf(rawValue));
    }
    if (TYPE_SINGLE_CHECKBOX.equals(definition.getType())) {
      return rawValue; // keep the boolean as-is
    }
    return String.valueOf(rawValue);
  }

  private Object toElement(CustomField definition, Object element) {
    if (isSelect(definition)) {
      return toOptionValue(definition, String.valueOf(element));
    }
    return String.valueOf(element); // repeatable text → plain String, no wrapper
  }

  private boolean isSelect(CustomField definition) {
    return definition.getSelectField() != null;
  }

  private CustomFieldOptionValue toOptionValue(CustomField definition, String optionId) {
    return CustomFieldOptionValue.builder()
      .id(optionId)
      .label(resolveOptionLabel(definition, optionId))
      .build();
  }

  private String resolveOptionLabel(CustomField definition, String optionId) {
    return Optional.ofNullable(definition.getSelectField())
      .map(SelectField::getOptions)
      .map(SelectFieldOptions::getValues)
      .orElseGet(List::of).stream()
      .filter(option -> optionId.equals(option.getId()))
      .map(SelectFieldOption::getValue)
      .filter(StringUtils::isNotBlank)
      .findFirst()
      .orElse(optionId);
  }
}
