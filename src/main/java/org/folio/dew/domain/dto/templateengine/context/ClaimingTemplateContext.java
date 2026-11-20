package org.folio.dew.domain.dto.templateengine.context;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Template context of a claims export: same structure as {@link OrderingTemplateContext},
 * but every order line is a {@link ClaimOrderLineContext} carrying its claimed pieces.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ClaimingTemplateContext extends BaseTemplateContext {
}
