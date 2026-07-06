package org.folio.dew.batch.acquisitions.services;

import org.folio.dew.domain.dto.acquisitions.customfields.CustomField;
import org.folio.dew.domain.dto.acquisitions.customfields.SelectField;
import org.folio.dew.domain.dto.acquisitions.customfields.SelectFieldOption;
import org.folio.dew.domain.dto.acquisitions.customfields.SelectFieldOptions;
import org.folio.dew.domain.dto.templateengine.context.CustomFieldContext;
import org.folio.dew.domain.dto.templateengine.context.CustomFieldOptionValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomFieldsServiceTest {

  private static final String ENTITY_TYPE = "po_line";

  @Mock
  private CustomFieldDefinitionService definitionService;

  @InjectMocks
  private CustomFieldsService service;

  @Test
  void resolve_nullOrEmptyRaw_returnsEmptyMap() {
    assertThat(service.resolve(null, ENTITY_TYPE)).isEmpty();
    assertThat(service.resolve(Map.of(), ENTITY_TYPE)).isEmpty();
  }

  @Test
  void resolve_singleSelect_scalarValueIsOptionWithLabel() {
    when(definitionService.getDefinitionsByRefId(ENTITY_TYPE))
      .thenReturn(Map.of("format", select("format", "Format", "opt_1", "Hardcover", "opt_2", "Paperback")));

    var result = service.resolve(raw("format", "opt_2"), ENTITY_TYPE);

    var ctx = result.get("format");
    assertThat(ctx.getName()).isEqualTo("Format");
    assertThat(ctx.getType()).isEqualTo("SINGLE_SELECT_DROPDOWN");
    assertThat(ctx.getValues()).isNull();
    assertThat(ctx.getValue()).isInstanceOf(CustomFieldOptionValue.class);
    var option = (CustomFieldOptionValue) ctx.getValue();
    assertThat(option.getId()).isEqualTo("opt_2");
    assertThat(option.getLabel()).isEqualTo("Paperback");
  }

  @Test
  void resolve_singleSelect_unknownOptionId_fallsBackToId() {
    when(definitionService.getDefinitionsByRefId(ENTITY_TYPE))
      .thenReturn(Map.of("format", select("format", "Format", "opt_1", "Hardcover")));

    var result = service.resolve(raw("format", "opt_9"), ENTITY_TYPE);

    var option = (CustomFieldOptionValue) result.get("format").getValue();
    assertThat(option.getId()).isEqualTo("opt_9");
    assertThat(option.getLabel()).isEqualTo("opt_9");
  }

  @Test
  void resolve_multiSelect_valuesAreOptionsWithLabels() {
    when(definitionService.getDefinitionsByRefId(ENTITY_TYPE))
      .thenReturn(Map.of("langs", select("langs", "Languages", "opt_1", "English", "opt_2", "German")));

    var result = service.resolve(raw("langs", List.of("opt_1", "opt_2")), ENTITY_TYPE);

    var ctx = result.get("langs");
    assertThat(ctx.getValue()).isNull();
    assertThat(ctx.getValues()).hasSize(2);
    assertThat(ctx.getValues()).allMatch(CustomFieldOptionValue.class::isInstance);
    var first = (CustomFieldOptionValue) ctx.getValues().get(0);
    assertThat(first.getId()).isEqualTo("opt_1");
    assertThat(first.getLabel()).isEqualTo("English");
    var second = (CustomFieldOptionValue) ctx.getValues().get(1);
    assertThat(second.getLabel()).isEqualTo("German");
  }

  @Test
  void resolve_repeatableText_valuesArePlainStrings() {
    when(definitionService.getDefinitionsByRefId(ENTITY_TYPE))
      .thenReturn(Map.of("keywords", text("keywords", "Keywords", "TEXTBOX_SHORT")));

    var result = service.resolve(raw("keywords", List.of("alpha", "beta")), ENTITY_TYPE);

    var ctx = result.get("keywords");
    assertThat(ctx.getValue()).isNull();
    assertThat(ctx.getValues()).containsExactly("alpha", "beta");
  }

  @Test
  void resolve_checkbox_valueIsBoolean() {
    when(definitionService.getDefinitionsByRefId(ENTITY_TYPE))
      .thenReturn(Map.of("urgent", text("urgent", "Urgent", "SINGLE_CHECKBOX")));

    var result = service.resolve(raw("urgent", Boolean.TRUE), ENTITY_TYPE);

    assertThat(result.get("urgent").getValue()).isEqualTo(Boolean.TRUE);
  }

  @Test
  void resolve_textbox_valueIsString() {
    when(definitionService.getDefinitionsByRefId(ENTITY_TYPE))
      .thenReturn(Map.of("note", text("note", "Note", "TEXTBOX_LONG")));

    var result = service.resolve(raw("note", "Line 1\nLine 2"), ENTITY_TYPE);

    assertThat(result.get("note").getValue()).isEqualTo("Line 1\nLine 2");
  }

  @Test
  void resolve_dropsUnknownRefId_nullValue_andHiddenFields() {
    var hidden = text("hidden", "Hidden", "TEXTBOX_SHORT");
    hidden.setVisible(false);
    var visible = text("kept", "Kept", "TEXTBOX_SHORT");
    when(definitionService.getDefinitionsByRefId(ENTITY_TYPE))
      .thenReturn(Map.of("hidden", hidden, "kept", visible));

    Map<String, Object> raw = new HashMap<>();
    raw.put("hidden", "secret");     // definition hidden → dropped
    raw.put("kept", "shown");        // kept
    raw.put("unknown", "orphan");    // no definition → dropped
    raw.put("nullValue", null);      // null → dropped

    var result = service.resolve(raw, ENTITY_TYPE);

    assertThat(result).containsOnlyKeys("kept");
    assertThat(result.get("kept").getValue()).isEqualTo("shown");
  }

  @Test
  void resolve_visibleUnsetOrTrue_areRendered() {
    var unset = text("unset", "Unset", "TEXTBOX_SHORT"); // visible == null
    var explicit = text("explicit", "Explicit", "TEXTBOX_SHORT");
    explicit.setVisible(true);
    when(definitionService.getDefinitionsByRefId(ENTITY_TYPE))
      .thenReturn(Map.of("unset", unset, "explicit", explicit));

    var result = service.resolve(raw2("unset", "a", "explicit", "b"), ENTITY_TYPE);

    assertThat(result).containsOnlyKeys("unset", "explicit");
  }

  @Test
  void resolve_returnsUnmodifiableMap() {
    when(definitionService.getDefinitionsByRefId(ENTITY_TYPE))
      .thenReturn(Map.of("kept", text("kept", "Kept", "TEXTBOX_SHORT")));

    var result = service.resolve(raw("kept", "shown"), ENTITY_TYPE);

    assertThatThrownBy(() -> result.put("x", CustomFieldContext.builder().build()))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  // ---- helpers ----

  private static Map<String, Object> raw(String key, Object value) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put(key, value);
    return map;
  }

  private static Map<String, Object> raw2(String k1, Object v1, String k2, Object v2) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put(k1, v1);
    map.put(k2, v2);
    return map;
  }

  private static CustomField text(String refId, String name, String type) {
    var cf = new CustomField();
    cf.setRefId(refId);
    cf.setName(name);
    cf.setType(type);
    return cf;
  }

  private static CustomField select(String refId, String name, String... idLabelPairs) {
    var cf = text(refId, name, "SINGLE_SELECT_DROPDOWN");
    var options = new SelectFieldOptions();
    var values = new java.util.ArrayList<SelectFieldOption>();
    for (int i = 0; i < idLabelPairs.length; i += 2) {
      var option = new SelectFieldOption();
      option.setId(idLabelPairs[i]);
      option.setValue(idLabelPairs[i + 1]);
      values.add(option);
    }
    options.setValues(values);
    var selectField = new SelectField();
    selectField.setOptions(options);
    cf.setSelectField(selectField);
    return cf;
  }
}