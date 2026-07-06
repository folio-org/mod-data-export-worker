package org.folio.dew.domain.dto.templateengine.context;

import java.util.List;

public record OrderWrapper(OrderContext order, List<OrderLineWrapper> orderLines) {
}
