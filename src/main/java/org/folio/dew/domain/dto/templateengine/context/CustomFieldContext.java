package org.folio.dew.domain.dto.templateengine.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Template-ready representation of a single custom-field value on a purchase order or PO line.
 * <ul>
 *   <li>{@code value} carries a scalar: {@link CustomFieldOptionValue} for a single-select,
 *       {@code Boolean} for a checkbox, or {@code String} for textbox/date/number fields.</li>
 *   <li>{@code values} carries an array: {@link CustomFieldOptionValue} elements for
 *       multi-select / repeatable select fields, or plain {@code String} elements for
 *       repeatable text fields.</li>
 * </ul>
 * Exactly one of {@code value} / {@code values} is populated per field.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomFieldContext {
  private String name;         // display name from definition.name
  private String type;         // definition type, e.g. SINGLE_SELECT_DROPDOWN / SINGLE_CHECKBOX / TEXTBOX_LONG
  private Object value;        // scalar: CustomFieldOptionValue | Boolean | String
  private List<Object> values; // array: CustomFieldOptionValue elements or plain String elements
}
