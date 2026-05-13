package org.folio.dew.domain.dto.templateengine;

import java.util.List;

public record OrderWrapper(OrderContext order, List<OrderLineWrapper> orderLines) {
}
