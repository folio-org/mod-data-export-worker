package org.folio.dew.domain.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Data
@NoArgsConstructor
public class UserId {
  private String userId;

  public String getUserId() {
    return userId.replace("\"", EMPTY);
  }
}
