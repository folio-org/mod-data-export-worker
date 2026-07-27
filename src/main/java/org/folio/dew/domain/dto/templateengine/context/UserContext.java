package org.folio.dew.domain.dto.templateengine.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserContext {
  private String id;
  private String firstName;
  private String lastName;
  private String fullName;
}
