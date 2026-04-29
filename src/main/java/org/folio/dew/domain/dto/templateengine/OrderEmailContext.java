package org.folio.dew.domain.dto.templateengine;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderEmailContext {

  private List<OrderWrapper> orders;
}
