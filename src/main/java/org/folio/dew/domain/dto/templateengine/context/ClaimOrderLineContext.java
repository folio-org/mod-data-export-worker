package org.folio.dew.domain.dto.templateengine.context;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ClaimOrderLineContext extends OrderLineContext {
  private List<PieceContext> pieces; // claimed pieces of this order line only
}
